import gtk
import csv
# the application main window launches lists which in tern launches
# dialogs which in turn are made from items
from items import *

# All windows will have the same size
SZ=(600,400)

class Dialog(object):
    """"Dialog box for editing values. Assuming all values are float.
    Each value has a label in the GUI and an attribute in this object."""
    #List below the labels, names and types of the attributes
    labels = []
    attributes = []
    types = []
    def __init__(self,parentDialog):
        self.parentDialog=parentDialog
    def make_dialog(self,parent_window,OKCancel=True):
        """Make the edit dialog box without running it. This can be extended
        by sub class
        """
        # Dont use gtk.Dialog and dont use modal because Nokia
        win=gtk.Window()
        win.set_modal(True)
        win.connect("delete_event", self.delete_event)
        win.connect("destroy", self.destroy)
        win.vbox=gtk.VBox()
        win.add(win.vbox)
        win.vbox.show()
        if OKCancel:
            win.hbox = gtk.HBox()
            win.bOK = gtk.Button('OK')
            win.bOK.connect('clicked', self.ok_event)
            win.bCancel = gtk.Button('Cancel')
            win.bCancel.connect('clicked', self.cancel_event)
            win.hbox.pack_start(win.bOK, True, True)
            win.bOK.show()
            win.hbox.pack_start(win.bCancel, True, True)
            win.bCancel.show()
            win.vbox.pack_end(win.hbox, False)
            win.hbox.show()
        self.dialog=win
        self.dialog.set_size_request(*SZ)

    def delete_event(self, widget, event, data=None):
        return False
    def destroy(self, widget, data=None):
        pass
    def cancel_event(self, widget, data=None):
        self.dialog.destroy()
    def ok_event(self, widget, data=None):
        try:
            temp_value=[]
            for i,attr in enumerate(self.attributes):
                # find the class of the attribue
                cls=self.types[i]
                entry=self.entries[i]
                # cast the text in the Entry widget to the class
                # this could generate an exception on a bad entry
                value=cls(entry.get_text())
                temp_value.append(value)
            for attr,value in zip(self.attributes,temp_value):
                self.__setattr__(attr,value)
            self.dialog.destroy()
            self.parentDialog.updateobj(self)
        except:
            # If there were problems, dont destroy the window and the user will
            # have to continue and play with it.
            pass
    def run(self,parent_window):
        self.make_dialog(parent_window)

        table = gtk.Table(3, 2, False)		
        self.entries=[]
        for r,l in enumerate(self.labels):
            attr=self.__getattribute__(self.attributes[r])

            label = gtk.Label(l)
            label.set_alignment(0, 0)
            table.attach(label,0,1,r,r+1)
            label.show()
            entry=attr.entry(self)
            self.entries.append(entry)
            table.attach(entry,1,2,r,r+1)
            entry.show()

        self.dialog.vbox.pack_start(table, False, False, 0)
        table.show()
        self.dialog.show()

    def newvalues(self):
        for i,value in enumerate(self.values):
            self.__setattr__(self.attributes[i],
                             self.types[i](value))
class OptionsDialog(Dialog):
    """"Dialog box for editing options values."""
    labels = ["Metabolism (KCal/Kg/day)","Goal weight (Kg)","History (days)"]
    attributes = ["met","weight","history"]
    types=[MyFloat,MyFloat,MyInt]
    values=[18.,77.,30]
    def __init__(self,parentDialog):
        Dialog.__init__(self,parentDialog)
        try:
            self.load()
            self.defined=True
        except IOError:
            self.newvalues()
            self.defined=True
    def save(self):
        f = open("fitness_options.csv","wb")
        csv.writer(f).writerow([self.__getattribute__(attr) for attr in self.attributes])
        f.close()
    def load(self):
        f = open("fitness_options.csv","rb")
        if not f: return False
        r = csv.reader(f)
        for row in r:
            for i,value in enumerate(row):
                value=self.types[i](value)
                self.__setattr__(self.attributes[i],value)
        f.close()
        return True

class DateObj(Dialog):
    """An object that contains information that is assigned to a specifice date.
    For example: Weight, food eating, Physical Activity.
    It is possible for multiple objects to have the same date
    """
    def __init__(self,parentDialog):
        Dialog.__init__(self,parentDialog)
    def newvalues(self):
        self.date=Date(datetime.date.today())
        Dialog.newvalues(self)
    def date_callback(self, widget):
        """Allow entrance of new date when the date button is pressed"""
        dt=self.date
        dialog = hildon.CalendarPopup(self.dialog, dt.dt.year, dt.dt.month, dt.dt.day)
        dialog.run()
        dt1=dialog.get_date()
        dt=Date(dt1[0],dt1[1],dt1[2])
        dialog.destroy()
        widget.set_label(str(dt)) # date2string(dt))
        self.date=dt
    def make_dialog(self,parent_window):
        Dialog.make_dialog(self,parent_window)

        button = gtk.Button()
        button.set_label(str(self.date))
        button.connect("clicked", self.date_callback)
        self.dialog.vbox.pack_start(button, False, False, 0)
        button.show()
    def __cmp__(self,other):
        """Comparing two DateObj is done by comparing their dates. This is needed
        in order to sort the list of objects which is held in DateObjList"""
        return cmp(self.date,other.date)
    def save(self,w):
        w.writerow([`self.date`]+[self.__getattribute__(attr) for attr in self.attributes])
    def load(self,row):
        types=[Date]+self.types
        attrs=['date']+self.attributes
        for i,value in enumerate(row):
            value=types[i](value)
            self.__setattr__(attrs[i],value)
    def run(self,parent_window):
        Dialog.run(self,parent_window)
        self.entries[0].grab_focus()

class Weight(DateObj):
    """Single weight entry"""
    labels = ["Weight"]
    attributes = ["weight"]
    types=[MyFloat]
    values=[0.] # use latest value

class Cal(DateObj):
    """Single cal entry"""
    labels = ["Desc","Quantity","Unit","Cal/Unit"]
    attributes = ["desc","quant","unit","calunit"]
    values = ["", 0., "", 0.]
    def cals(self):
        return self.quant * self.calunit

class PA(Cal):
    types=[Completion,MyFloat,PAUnit,MyFloat]

class Food(Cal):
    types=[Completion,MyFloat,FoodUnit,MyFloat]

