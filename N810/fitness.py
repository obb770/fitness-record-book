#!/usr/bin/env python2.5
# The below license appears in the About dialog-box
license = """Fitness Record Book
Copyright 2000-4,2007 Ehud (Udi) Ben-Reuven
Derived from:
Copyright 1997 Eric W. Sink

LEGAL DISCLAIMER - The author(s) of this software are not medical
practitioners of any kind.  We do not have the education, experience,
license, credentials or desire to provide health-related advice.  You
should consult a physician before undertaking any activities which are
intended to improve your health.  By using this software, you agree
that we cannot be held responsible for any changes in your physical
condition or health.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software 
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
"""
import gtk
import hildon
import time
import datetime
import csv
# simulate data for debugging
sim=False
if sim:
    import random
    rand = random.Random()

# All windows will have the same size
SZ=(600,400)

class Dialog(object):
    """"Dialog box for editing values. Assuming all values are float.
    Each value has a label in the GUI and an attribute in this object."""
    #List below the labels and the names of the attributes
    labels = []
    attributes = []
    def make_dialog(self,parent_window):
        """Make the edit dialog box without running it. This can be extended
        by sub class
        """
        dialog=gtk.Dialog("Edit",parent_window,
                          gtk.DIALOG_MODAL | gtk.DIALOG_DESTROY_WITH_PARENT |
                          gtk.DIALOG_NO_SEPARATOR,
                          (gtk.STOCK_CANCEL, gtk.RESPONSE_REJECT,
                           gtk.STOCK_OK, gtk.RESPONSE_ACCEPT
                       ))
        self.dialog=dialog
        dialog.set_size_request(*SZ)

    def run(self,parent_window):
        self.make_dialog(parent_window)

        table = gtk.Table(3, 2, False)		
        entries=[]
        for r,l in enumerate(self.labels):
            attr=self.__getattribute__(self.attributes[r])
            
            label = gtk.Label(l)
            label.set_alignment(0, 0)
            table.attach(label,0,1,r,r+1)
            label.show()
            
            try:
                entry=attr.entry()
            except:
                entry = gtk.Entry()
                if isinstance(attr,float):
                    txt='%.1f'%attr
                else:
                    txt=str(attr)
                entry.set_text(txt)
            entries.append(entry)
            table.attach(entry,1,2,r,r+1)
            entry.show()

        self.dialog.vbox.pack_start(table, False, False, 0)
        table.show()

        while True:
            # Loop until Cancel
            if self.dialog.run()!=gtk.RESPONSE_ACCEPT: break
            # or until all inputs are valid
            try:
                temp_value=[]
                for attr,entry in zip(self.attributes,entries):
                    # find the class of the attribue
                    cls=type(self.__getattribute__(attr))
                    # cast the text in the Entry widget to the class
                    # this could generate an exception on a bad entry
                    value=cls(entry.get_text())
                    temp_value.append(value)
            except:
                continue
            for attr,value in zip(self.attributes,temp_value):
                self.__setattr__(attr,value)
            break
        self.dialog.destroy()

class OptionsDialog(Dialog):
    """"Dialog box for editing options values."""
    labels = ["Metabolism (KCal/Kg/day)","Goal weight (Kg)","History (days)"]
    attributes = ["met","weight","history"]
    types=[float,float,int]
    def __init__(self):
        try:
            self.load()
            self.defined=True
        except:
            self.met=18.
            self.weight=77.
            self.history=30
            self.defined=False
    def save(self):
        f = open("fitness_options.csv","wb")
        csv.writer(f).writerow([self.__getattribute__(attr) for attr in self.attributes])
        f.close()
    def load(self):
        f = open("fitness_options.csv","rb")
        r = csv.reader(f)
        for row in r:
            for i,value in enumerate(row):
                value=self.types[i](value)
                self.__setattr__(self.attributes[i],value)
        f.close()

class Date(object):
    # repr and init must use same format
    fmt='%d-%b-%y'
    def __init__(self,year,month=None,day=None):
        """Create using either a 3-tuplet or a string with the exact same
	format used in repr"""
        if isinstance(year,datetime.date):
            self.dt=year
        else:
            if isinstance(year,str):
                s=year
                t=time.strptime(s,self.fmt)
                year,month,day = t[0:3]
            self.dt=datetime.date(year,month,day)
        assert self.dt
    def __repr__(self):
        """ US string representation of date.
	TODO: get local format from OS"""
        return self.dt.strftime(self.fmt)        
    def __str__(self):
        """ US string representation of date.
	TODO: get local format from OS"""
        return self.dt.strftime('%m/%d/%y')
    def __cmp__(self,other):
        return cmp(self.dt,other.dt)
    def __sub__(self,other):
        return self.dt-other.dt
    
class DateObj(Dialog):
    """An object that contains information that is assigned to a specifice date.
    For example: Weight, food eating, Physical Activity.
    It is possible for multiple objects to have the same date
    """
    def today(self):
        self.date=Date(datetime.date.today())
    def rand(self):
        """ Pick a random date between 1/1/1998 and today """
        dt=datetime.date.today()
        year = rand.randint(1998, dt.year)
        if year == dt.year:
            month=rand.randint(1, dt.month)
            if month == dt.month:
                day = rand.randint(1, dt.day)
            else:
                day = rand.randint(1, 20)
        else:
            month = rand.randint(1, 12)
            day = rand.randint(1, 20)	
        self.date=Date(year,month,day)
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

class DateObjList(object):
    """Managing objects that have a date field
    """
    # When sublcassing, override the following: 
    title="Date"
    objclass=DateObj
    column_names = ['Date']
    fname="fitness_dates.csv"
    def load(self):
        f = open(self.fname,"rb")
        r = csv.reader(f)
        for row in r:
            obj = self.objclass()
            obj.load(row)
            self.liststore.append([obj])
        f.close()
    def save(self):
        f = open(self.fname,"wb")
        w=csv.writer(f)
        for row in self.liststore:
            row[0].save(w)
        f.close()
    def __init__(self):
        # When subclassing, override the tuple with appropriate method to
        # display the contnet of each column
        self.cell_data_funcs = (self.cell_date,) #Note that this must be a tuple
        self.liststore = gtk.ListStore(object)
        
        try:
            self.load()
        except:
            if sim:
                for i in range(50):
                    obj = self.objclass()
                    obj.rand()
                    self.liststore.append([obj])
    def cell_date(self, column, cell, model, iter):
        """Extract the date string from each object in the list, and place it
	in a GUI cell which is part of the Date column
	"""
        obj=model.get_value(iter, 0)
        cell.set_property('text', str(obj.date))
    def total_event(self,widget):
        """Return to the main window when the Total button is pressed"""
        self.w.response(gtk.RESPONSE_OK)
    def delete_event(self, widget, event, data=None):
        """Return to main window when the window is closed"""
        return self.total_event(widget)
    def new_event(self,widget):
        """Add a new DateObj to the list when the New button is pressed"""
        obj = self.objclass()
        obj.today()
        self.liststore.append([obj])
        self.edit_obj(obj)        
    def date_sort(self, model, iter1, iter2):
        """Sort method used to keep the objects in the list sorted in descending
	order """
        obj1=model.get_value(iter1, 0)
        obj2=model.get_value(iter2, 0)
        if obj1 and obj2:
            return cmp(obj1,obj2)
        else:
            return 1 #When adding a new entry, one of the objs is None.
    def run(self,parent_window):
        """Run the dialog window for managing the list of objects
	parent_window - the window from which this window was launched
	"""
        self.parent_window=parent_window
        win=gtk.Dialog(self.title,parent_window,
                  gtk.DIALOG_MODAL | gtk.DIALOG_DESTROY_WITH_PARENT |
                  gtk.DIALOG_NO_SEPARATOR,
                  )
        self.w=win
        win.set_size_request(*SZ)

        # create the TreeView
        sm = gtk.TreeModelSort(self.liststore)
        sm.set_sort_func(0,self.date_sort)
        sm.set_sort_column_id(0, gtk.SORT_DESCENDING)
        self.treeview = gtk.TreeView(sm)

        # create the TreeViewColumns to display the data
        self.tvcolumn = [None] * len(self.column_names)
        for n in range(len(self.column_names)):
            cell = gtk.CellRendererText()
            self.tvcolumn[n] = gtk.TreeViewColumn(self.column_names[n], cell)
            self.tvcolumn[n].set_cell_data_func(cell, self.cell_data_funcs[n])
            self.treeview.append_column(self.tvcolumn[n])

        self.treeview.connect('row-activated', self.edit)
        self.scrolledwindow = gtk.ScrolledWindow()
        self.scrolledwindow.add(self.treeview)
        win.vbox.pack_start(self.scrolledwindow)

        # Add Total/New buttons at the bottom
        win.hbox = gtk.HBox()
        win.bTotal = gtk.Button('Total')
        win.bTotal.connect('clicked', self.total_event)
        win.bNew = gtk.Button('New')
        win.bNew.connect('clicked', self.new_event)
        win.hbox.pack_start(win.bTotal, True, True)
        win.hbox.pack_start(win.bNew, True, True)
        win.vbox.pack_start(win.hbox, False)

        win.show_all()
        # start a recursive mainloop.
        # the mainloop will end only when the Total button is pressedn
        # so there is no need to check the response_id
        win.run()
        self.w.destroy()
    def edit(self, treeview, path, column):
        """Edit an entry when an item in the list is double clicked"""
        model = treeview.get_model()
        iter = model.get_iter(path)
        obj= model.get_value(iter, 0)
        self.edit_obj(obj)
    def edit_obj(self,obj):
        # run the edit dialog of the object
        obj.run(self.parent_window)
        # The date of the object may have been changed and the entire list
        # needs to be resorted.
        # Rebuild the sorted list of objects and plug them into tree view.
        sm = gtk.TreeModelSort(self.liststore)
        sm.set_sort_func(0,self.date_sort)
        sm.set_sort_column_id(0, gtk.SORT_DESCENDING)
        self.treeview.set_model(sm)

class Weight(DateObj):
    """Single weight entry"""
    labels = ["Weight"]
    attributes = ["weight"]
    types=[float]
    weight=0.
    def rand(self):
        DateObj.rand(self)
        self.weight=float(rand.randint(78, 108))

class WeightList(DateObjList):
    """Manage all weight entries"""
    objclass=Weight
    title="Weight"
    column_names = ['Date', 'Weight']
    fname="fitness_weights.csv"

    def __init__(self):
        DateObjList.__init__(self)
        # This should come after the super's init because it overrides it.
        self.cell_data_funcs = (self.cell_date, self.cell_weight)
    def last_weight(self):
        """ Return the latest weight value this is used to estimate the current
	metabolisem of your body"""
        maxobj=None
        for row in self.liststore:
            obj=row[0]
            if not maxobj or obj > maxobj:
                maxobj = obj
        if maxobj:
            return maxobj.weight
        else:
            # TODO take value from goal weight
            return 81.
    def cell_weight(self, column, cell, model, iter):
        """Extract the weight string from each object in the list, and place it
	in a GUI cell which is part of the Weight column"""
        obj=model.get_value(iter, 0)
        cell.set_property('text', '%.1f'%obj.weight)

class Cal(DateObj):
    """Single cal entry"""
    labels = ["Desc","Quantity","Unit","Cal/Unit"]
    attributes = ["desc","quant","unit","calunit"]
    types=[str,float,None,float]
    desc=""
    quant=0.
    unit=""
    calunit=0.
    def cals(self):
        return self.quant * self.calunit


class CalList(DateObjList):
    """Manage all Cal entries"""
    objclass=Cal
    title="Cal"
    column_names = ['Date', 'Desc', 'Cal']

    def __init__(self):
        DateObjList.__init__(self)
        # This should come after the super's init because it overrides it.
        self.cell_data_funcs = (self.cell_date, self.cell_desc, self.cell_cal)
    def cell_desc(self, column, cell, model, iter):
        """Extract the description string from each object in the list,
        and place it in a GUI cell which is part of the Desc column"""
        obj=model.get_value(iter, 0)
        cell.set_property('text', obj.desc)
    def cell_cal(self, column, cell, model, iter):
        """Compute the total calories from each object in the list, and place it
	in a GUI cell which is part of the Cal column"""
        obj=model.get_value(iter, 0)
        cell.set_property('text', '%.1f'%(obj.cals()))    
    def cal_in_range(self,sdate,edate):
        """ Return the sum of calories inside the date range"""
        calsum=0.
        for row in self.liststore:
            obj=row[0]
            if obj.date >= sdate and obj.date <= edate:
                calsum += obj.cals()
        return calsum
class Combo(object):
    list=[]
    active=-1
    def __init__(self,txt=None):
        for idx,l in enumerate(self.list):
            if l==txt:
                self.active=idx
                break
    def __str__(self):
        return self.list[self.active]
    def entry(self):
        e=gtk.combo_box_new_text()
        for l in self.list:
            e.append_text(l)
        e.set_active(self.active)
        # All other entries in the dialog box will be text entries (gtk.Entry)
        # and their text value will be taken using get_text method.
        e.get_text=e.get_active_text
        return e
class PAUnit(Combo):
    list=["Item","Minute","Mile"]
class PA(Cal):
    unit=PAUnit()
    types=[str,float,PAUnit,float]
    def rand(self):
        Cal.rand(self)        
        self.desc="running"
        self.quant=float(rand.randint(50, 100))
        self.unit=PAUnit("Minute")
        self.calunit=13.
        
class PAList(CalList):
    objclass=PA
    title="PA"
    fname="fitness_pas.csv"

class FoodUnit(Combo):
    list=['Item','Tsp','Tbsp','Cup','Ounce','Slice','Bowl']
class Food(Cal):
    types=[str,float,FoodUnit,float]    
    unit=FoodUnit()
    def rand(self):
        Cal.rand(self)
        self.desc="yogurt"
        self.quant=rand.randint(10, 100)/10.
        self.unit=FoodUnit("Ounce")
        self.calunit=6.

class FoodList(CalList):
    objclass=Food
    title="Food"
    fname="fitness_foods.csv"

class AboutDialog():
    def __init__(self,window):
        dialog=gtk.Dialog("About",window,
                          gtk.DIALOG_MODAL | gtk.DIALOG_DESTROY_WITH_PARENT |
                          gtk.DIALOG_NO_SEPARATOR,
                          (gtk.STOCK_OK, gtk.RESPONSE_DELETE_EVENT)
                      )
        #sz=window.get_size()
        dialog.set_size_request(*SZ)


        scrolled_window = gtk.ScrolledWindow()
        #scrolled_window.set_border_width(10)
        scrolled_window.set_policy(gtk.POLICY_AUTOMATIC, gtk.POLICY_AUTOMATIC)

        label = gtk.Label(license)
        scrolled_window.add_with_viewport(label)
        label.show()

        dialog.vbox.pack_start(scrolled_window, True, True, 0)
        scrolled_window.show()

        dialog.run()
        dialog.destroy()

class FitnessApp(hildon.Program):
    def dialog_callback(self, widget,data):
        if data==0:
            self.foodDialog.run(self.window)
        elif data==1:
            self.paDialog.run(self.window)
        else:
            self.weightDialog.run(self.window)
        self.draw()
    def dtrange_callback(self, widget, data):
        if data==0:
            self.today()
        elif data==1:
            self.yesterday()
        else:
            self.week()
        self.draw()
    def date_callback(self, widget, data):
        if data:
            dt=self.sdate
        else:
            dt=self.edate

        dialog = hildon.CalendarPopup (self.window, dt.dt.year, dt.dt.month, dt.dt.day)
        dialog.run()
        dt1=dialog.get_date()
        dt=Date(dt1[0],dt1[1],dt1[2])
        dialog.destroy()
        widget.set_label(str(dt)) # date2string(dt))

        if data:
            self.sdate=dt
        else:
            self.edate=dt
        self.draw()
    def draw(self):
        self.sbutton.set_label(str(self.sdate)) # date2string(self.sdate))
        self.ebutton.set_label(str(self.edate)) # date2string(self.edate))
        days=(self.edate-self.sdate).days+1
        cal = self.foodDialog.cal_in_range(self.sdate,self.edate)
        pa = self.paDialog.cal_in_range(self.sdate,self.edate)
        met=days*self.weightDialog.last_weight()*self.optionsDialog.met
        net=met+pa-cal
        behav=net/days/self.optionsDialog.met
        left=net-days*self.optionsDialog.weight*self.optionsDialog.met
        self.values[0].set_text('%.1f'%cal)
        self.values[1].set_text('%.1f'%pa)
        self.values[2].set_text('%.1f'%met)
        self.values[3].set_text('%.1f'%net)
        self.values[4].set_text('%.1f'%behav)
        self.values[5].set_text(str(days))
        self.values[6].set_text('%.1f'%left)
    def save(self):
        self.optionsDialog.save()
        self.foodDialog.save()
        self.paDialog.save()
        self.weightDialog.save()
    def menuitem_response(self, widget, data):
        if data==0:
            self.optionsDialog.run(self.window)
            self.draw()
        elif data==1:
            self.save()
        else:
            AboutDialog(self.window)
            
    def today(self):
        t=datetime.date.today()
        self.sdate=Date(t.year,t.month,t.day)
        self.edate=Date(t.year,t.month,t.day)
    def yesterday(self):
        t=datetime.date.today()
        t-=datetime.timedelta(1)
        self.sdate=Date(t.year,t.month,t.day)
        self.edate=Date(t.year,t.month,t.day)
    def week(self):
        t=datetime.date.today()
        self.edate=Date(t.year,t.month,t.day)
        t-=datetime.timedelta(t.weekday())
        self.sdate=Date(t.year,t.month,t.day)
    def __init__(self):
        self.sdate=Date(2007,12,1)
        self.edate=Date(2007,12,1)

        hildon.Program.__init__(self)

        self.window = hildon.Window()
        self.window.set_size_request(*SZ)
        self.window.set_title("Fitness Record Book")
        self.window.connect("destroy", gtk.main_quit)
        self.add_window(self.window)

        menu = gtk.Menu()
        c=0
        for l in ["Options...","Save","About..."]:
            menu_items = gtk.MenuItem(l)
            menu.append(menu_items)
            menu_items.connect("activate", self.menuitem_response, c)
            c+=1
            menu_items.show()
        self.window.set_menu(menu)
        menu.show()

        table = gtk.Table(13, 3, True)
        r=0

        c=0
        for l in ["Today","Yesturday","This Week"]:
            button = gtk.Button(l)
            button.connect("clicked", self.dtrange_callback,c)
            table.attach(button,c,c+1,r,r+2)
            button.show()
            c+=1
        r+=2

        button = gtk.Button()
        self.sbutton=button
        button.connect("clicked", self.date_callback, True)
        table.attach(button,0,1,r,r+1)
        button.show()

        label = gtk.Label("thru")
        table.attach(label,1,2,r,r+1)
        label.show()

        button = gtk.Button()
        self.ebutton=button
        button.connect("clicked", self.date_callback, False)
        table.attach(button,2,3,r,r+1)
        button.show()
        r+=1

        self.values=[]
        for l in ["Calories In","PA Calories","Metabolism","Net Calories",
                  "Behaviorial Weight","Days in Range","Cals Left to Eat"]:
            label = gtk.Label(l)
            label.set_alignment(0, 0)
            table.attach(label,0,2,r,r+1)
            label.show()

            label = gtk.Label("0.0")
            label.set_alignment(0, 0)
            table.attach(label,2,3,r,r+1)
            label.show()
            self.values.append(label)

            r+=1

        for c,l in enumerate(["Food","PA","Weight"]):
            button = gtk.Button(l)
            button.connect("clicked", self.dialog_callback,c)
            table.attach(button,c,c+1,r,r+2)
            button.show()
        r=r+2

        self.window.add(table)
        table.show()
        self.window.show()
        self.optionsDialog = OptionsDialog()
        self.weightDialog = WeightList()
        self.paDialog=PAList()
        self.foodDialog=FoodList()

        self.load()
        if not self.optionsDialog.defined:
            self.optionsDialog.run(self.window)
    def load(self):
        self.today()
        self.draw()

    def run(self):
        gtk.main()

app = FitnessApp()
app.run()
