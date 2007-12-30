import gtk
import time
import datetime

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

    
class Completion2(object):
    # An envolpe to hold text that can be entered with gtk.Entry and completion
    # dont use gtk.EntryCompletion because the completion required by the
    # application is more involved. Also gtk.EntryCompletion does not allow
    # the parent dialog to be modal on Nokia
    def __init__(self,txt=''):
        self.txt=txt
        self.last_t=txt
    def entry(self,parentDialog):
        self.parentDialog=parentDialog
        entry = gtk.Entry()
        entry.set_text(self.txt)
        entry.connect("changed",self.changed_cb)
        return entry
    def changed_cb(self,entry):
        t=entry.get_text()
        p=min([len(t),entry.get_position()])
        print "Changed",t,p
        s=t[:p+1]
        if t==self.last_t: return
        entry.set_text(s)
        if not s: return
        for l in self.parentDialog.completion_liststore:
            r=str(l[0].desc)
            if r.startswith(s):
                # this must be before set_text because set_text will recursively
                # call this method
                self.last_t=r
                entry.set_text(r)
                entry.set_position(p)
                entry.show()
                break
    def __str__(self): # TODO who is using this
        return self.txt
   
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
