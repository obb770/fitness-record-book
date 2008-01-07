import gtk
import csv
# the application main window launches lists which in tern launches
# dialogs which in turn are made from items
from items import *
from dialogs import *

class DateObjList(Dialog):
    """Managing objects that have a date field
    """
    # When sublcassing, override the following: 
    title="Date"
    objclass=DateObj
    column_names = ['Date']
    fname="fitness_dates.csv"
    def updateobj(self,obj):
        if obj.is_new:
            self.liststore.append([obj])
        Dialog.updateobj(self,obj)
    def load(self):
        f = open(self.fname,"rb")
        r = csv.reader(f)
        for row in r:
            obj = self.objclass(self)
            obj.load(row)
            self.liststore.append([obj])
        f.close()
    def save(self):
        f = open(self.fname,"wb")
        w=csv.writer(f)
        for row in self.liststore:
            row[0].save(w)
        f.close()
    def __init__(self,parentDialog):
        self.parentDialog=parentDialog        
        # When subclassing, override the tuple with appropriate method to
        # display the contnet of each column
        self.cell_data_funcs = (self.cell_date,) #Note that this must be a tuple
        self.liststore = gtk.ListStore(object)

        try:
            self.load()
        except IOError:
            pass
    def cell_date(self, column, cell, model, iter):
        """Extract the date string from each object in the list, and place it
	in a GUI cell which is part of the Date column
	"""
        obj=model.get_value(iter, 0)
        cell.set_property('text', str(obj.date))
    def new_event(self,widget):
        """Add a new DateObj to the list when the New button is pressed"""
        obj = self.objclass(self)
        obj.newvalues()
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
        self.make_dialog(parent_window,OKCancel=False)
        win=self.dialog

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
        win.bTotal.connect('clicked', self.cancel_event)
        win.bNew = gtk.Button('New')
        win.bNew.connect('clicked', self.new_event)
        win.hbox.pack_start(win.bTotal, True, True)
        win.hbox.pack_start(win.bNew, True, True)
        win.vbox.pack_end(win.hbox, False)

        win.show_all()
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

class WeightList(DateObjList):
    """Manage all weight entries"""
    objclass=Weight
    title="Weight"
    column_names = ['Date', 'Weight']
    fname="fitness_weights.csv"

    def __init__(self,parentDialog):
        DateObjList.__init__(self,parentDialog)
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

class CalList(DateObjList):
    """Manage all Cal entries"""
    objclass=Cal
    title="Cal"
    column_names = ['Date', 'Desc', 'Cal']

    def __init__(self,parentDialog):
        # for all objects' names (desc item) keep the latest object
        self.dict={}
        # and build a liststore of these names
        self.dictlist=gtk.ListStore(str)
        DateObjList.__init__(self,parentDialog)
        # This should come after the super's init because it overrides it.
        self.cell_data_funcs = (self.cell_date, self.cell_desc, self.cell_cal)
    def loadobjname(self,obj):
        name=str(obj.desc)
        if name not in self.dict:
            self.dictlist.append([name])
        self.dict[name]=obj
    def updateobj(self,obj):
        self.loadobjname(obj)
        DateObjList.updateobj(self,obj)
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
    def edit_obj(self,obj):
        DateObjList.edit_obj(self,obj)
    def load(self):
        DateObjList.load(self)
        print 'reading file ',self.dict_fname
        f = open(self.dict_fname,"rb")
        r = csv.reader(f)
        for row in r:
            print row
            obj = self.objclass(self)
            obj.load(row)
            self.loadobjname(obj)
        f.close()
    def save(self):
        DateObjList.save(self)
        f = open(self.dict_fname,"wb")
        w=csv.writer(f)
        #for (name,obj) in self.dict.iteritems():
            #cls=obj.types[0]
            #attr=obj.attributes[0]
            #obj.__setattr__(attr)=cls(name)
        for obj in self.dict.itervalues():
            obj.save(w)
        f.close()
class PAList(CalList):
    objclass=PA
    title="PA"
    fname="fitness_pas.csv"
    dict_fname="fitness_pa_dict.csv"

class FoodList(CalList):
    objclass=Food
    title="Food"
    fname="fitness_foods.csv"
    dict_fname="fitness_food_dict.csv"
