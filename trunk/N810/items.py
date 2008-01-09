import gtk
import time
import datetime
try:
    import hildon
except:
    from hildonstub import hildon

class MyFloat(float):
    def entry(self,dialog):
        entry = gtk.Entry()
        self.setentry(entry)
        return entry
    def setentry(self,entry):
        txt='%.1f'%self
        entry.set_text(txt)

class MyInt(int):
    def entry(self,dialog):
        entry = gtk.Entry()
        self.setentry(entry)
        return entry
    def setentry(self,entry):
        txt=str(self)
        entry.set_text(txt)

# repr and init must use same format
datefmt='%d-%b-%y'
class MyDateEditor(hildon.DateEditor):
    def get_text(self):
        dt=datetime.date(*self.get_date())
        return dt.strftime(datefmt)
class Date(object):

    def __init__(self,year,month=None,day=None):
        """Create using either a 3-tuplet or a string with the exact same
	format used in repr"""
        if isinstance(year,datetime.date):
            self.dt=year
        elif year=="today":
            self.dt=datetime.date.today()
        else:
            if isinstance(year,str):
                s=year
                t=time.strptime(s,datefmt)
                year,month,day = t[0:3]
            self.dt=datetime.date(year,month,day)
        assert self.dt
    def __repr__(self):
        """ US string representation of date.
	TODO: get local format from OS"""
        return self.dt.strftime(datefmt)        
    def __cmp__(self,other):
        return cmp(self.dt,other.dt)
    def __sub__(self,other):
        return self.dt-other.dt
    def get_date(self):
        return (self.dt.year,self.dt.month,self.dt.day)
    def entry(self,dialog):
        entry=MyDateEditor()
        entry.set_date(*self.get_date())
        return entry
    def setentry(self,entry):
        # On purpose dont do anything because we dont want date to be modified
        # by completion selection
        pass
        
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
    def entry(self,dialog):
        e=gtk.combo_box_new_text()
        for l in self.list:
            e.append_text(l)
        e.set_active(self.active)
        # All other entries in the dialog box will be text entries (gtk.Entry)
        # and their text value will be taken using get_text method.
        e.get_text=e.get_active_text
        return e
    def setentry(self,entry):
        entry.set_active(self.active)
class PAUnit(Combo):
    list=["Item","Minute","Mile"]
class FoodUnit(Combo):
    list=['Item','Tsp','Tbsp','Cup','Ounce','Slice','Bowl']
  
class Completion(object):
    # An envolpe to hold text that can be entered with gtk.Entry and completion
    def __init__(self,txt=''):
        self.txt=txt
    def entry(self,dialog):
        self.dialog=dialog
        p=dialog.parentDialog
        self.parentDialog=p
        l=p.dictlist
        entry = gtk.Entry()
        entry.set_text(self.txt)
        completion = gtk.EntryCompletion()
        completion.set_inline_completion(True)
        completion.set_model(l)
        entry.set_completion(completion)
        completion.set_text_column(0)
        completion.connect("insert-prefix", self.insert_cb)
        return entry
    def insert_cb(self,completion,prefix):
        print 'insert',prefix 
        d=self.dialog
        p=self.parentDialog
        o=p.dict[prefix]
        for r,l in enumerate(d.attributes):
            attr=o.__getattribute__(l)
            attr.setentry(d.entries[r])
    def __str__(self):
        return self.txt
    def setentry(self,entry):
        pass
