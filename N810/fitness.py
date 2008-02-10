#!/usr/bin/env python2.5
# TODO dictonary of stored values should have a key based on description and
#      unit. For this to work there should be an interactive recall of data from
#      the dictonary which is based also on unit selected in dialog box
#      there is no problem in upgrading existing CSV files because the _dict
#      files already have the unit in each line.
# TODO Numeric fields should have the NumLock turned on by default.
#      This is done with gtk.GetEntry.set_input_mode which is a Hildon extension.
#      Sadly this is not in the current version of pymaemo c1.0-2
# TODO remove old records in DateObjList accoring to OptionsDialog.history
# TODO add to OptionsDialog the folder location for CSV files
# TODO DateObj.run.Del
# TODO current manual date range selection is buggy. Instead popup a dialog box in which Start/End dates are selected and validated.
# TODO validate date range
# TODO mark the location of input errors in dialog
# TODO localization

# The below license appears in the About dialog-box
license = """Fitness Record Book
2007-8 Ehud (Udi) Ben-Reuven & Ofer Barkai
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
try:
    import osso
except:
    from ossostub import osso
try:
    import hildon
except:
    from hildonstub import hildon

# The version and service name of the application
from os import getenv
version = getenv("FITNESS_VERSION", "<unknown>")
service = getenv("FITNESS_SERVICE", "fitness")

# the application main window launches lists which in tern launches
# dialogs which in turn are made from items
from items import *
from dialogs import *
from lists import *


class AboutDialog(gtk.AboutDialog):
    def __init__(self):
        gtk.AboutDialog.__init__(self)
        self.set_size_request(*SZ)
        self.set_logo_icon_name("fitness")
        self.set_name("Fitness Record Book")
        self.set_version(version)
        self.set_website("http://benreuven.com/udi/diet")
        self.set_comments("You must agree to the license\nbefore using this program")
        self.set_authors(["Eric W. Sink","Ehud (Udi) Ben-Reuven","Ofer Barkai"])
        self.set_copyright("""Copyright (c) 1997 Eric W. Sink\nCopyright (c) 2000-4 Ehud (Udi) Ben-Reuven\nCopyright (c) 2007-8 Ehud (Udi) Ben-Reuven & Ofer Barkai""")
        self.set_license(license)
        self.run()
        self.destroy()

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
    def draw(self):
        sdate=Date(*self.sbutton.get_date())
        edate=Date(*self.ebutton.get_date())

        days=(edate-sdate).days+1
        cal = self.foodDialog.cal_in_range(sdate,edate)
        pa = self.paDialog.cal_in_range(sdate,edate)
        met=days*self.weightDialog.last_weight()*self.optionsDialog.met
        b=cal-pa
        net=met-b
        behav=b/days/self.optionsDialog.met
        left=days*self.optionsDialog.weight*self.optionsDialog.met-b
        self.values[0].set_text('%.1f'%cal)
        self.values[1].set_text('%.1f'%pa)
        self.values[2].set_text('%.1f'%met)
        self.values[3].set_text('%.1f'%net)
        self.values[4].set_text('%.1f'%behav)
        self.values[5].set_text(str(days))
        self.values[6].set_text('%.1f'%left)

    def save(self, user_data=None):
        self.optionsDialog.save()
        self.foodDialog.save()
        self.paDialog.save()
        self.weightDialog.save()
    def newfood_response(self, widget):
        self.foodDialog.parent_window=self.window
        self.foodDialog.dialog=self.window
        self.foodDialog.new_event(None)
    def save_response(self, widget):
            self.force_save()        
    def menuitem_response(self, widget, data):
        if data==1:
            self.optionsDialog.run(self.window)
            #self.draw()
        elif data==0:
            self.force_save()
        else:
            AboutDialog()

    def today(self):
        t=datetime.date.today()
        self.sbutton.set_date(t.year,t.month,t.day)
        self.ebutton.set_date(t.year,t.month,t.day)
    def yesterday(self):
        t=datetime.date.today()
        t-=datetime.timedelta(1)
        self.sbutton.set_date(t.year,t.month,t.day)
        self.ebutton.set_date(t.year,t.month,t.day)
    def week(self):
        t=datetime.date.today()
        self.ebutton.set_date(t.year,t.month,t.day)
        t-=datetime.timedelta(t.weekday())
        self.sbutton.set_date(t.year,t.month,t.day)

    # Called whenever the application is sent to background or
    # get to foreground. If it goes to background, calls
    # 
    def topmost_change(self, arg, user_data):
        if self.get_is_topmost():
            self.set_can_hibernate(False)
        else:
            self.autosave.force_autosave()
            self.set_can_hibernate(True)
    def updateobj(self,obj):
        self.autosave.userdata_changed()
        self.draw()
    def force_save(self):
        self.autosave.force_autosave()
    def quit(self, evt):
        self.force_save()
        gtk.main_quit()
    def date_change(self,gobject, property_spec,isstart):
        self.draw()
    def __init__(self):
        hildon.Program.__init__(self)
        self.context = osso.Context(service, version, False)
        self.autosave = osso.Autosave(self.context)
        # because of bug in osso.Autosave you must pass a call-back data
        self.autosave.set_autosave_callback(self.save,1)
        self.connect("notify::is-topmost", self.topmost_change)
        
        self.window = hildon.Window()
        self.window.set_size_request(*SZ)
        self.window.set_title("Fitness Record Book")
        self.window.connect("destroy", self.quit)
        self.add_window(self.window)

        menu = gtk.Menu()
        c=0
        # put Save first
        for l in ["Save","Options...","About..."]:
            menu_items = gtk.MenuItem(l)
            menu.append(menu_items)
            menu_items.connect("activate", self.menuitem_response, c)
            c+=1
            menu_items.show()
        self.window.set_menu(menu)
        menu.show()

        table = gtk.Table(11, 3, False)
        r=0

        c=0
        for l in ["Today","Yesterday","This Week"]:
            button = gtk.Button(l)
            button.connect("clicked", self.dtrange_callback,c)
            table.attach(button,c,c+1,r,r+2)
            button.show()
            c+=1
        r+=2

        button = hildon.DateEditor()
        self.sbutton=button
        # FIXME there is no event to DateEditor indicating that the date has changed
        button.connect("notify::year", self.date_change,True)
        button.connect("notify::month",self.date_change,True)
        button.connect("notify::day",self.date_change,True)
        table.attach(button,0,1,r,r+1)
        button.show()

        label = gtk.Label("thru")
        table.attach(label,1,2,r,r+1)
        label.show()

        button = hildon.DateEditor()
        self.ebutton=button
        # FIXME there is no event to DateEditor indicating that the date has changed
        button.connect("notify::year",self.date_change,False)
        button.connect("notify::month",self.date_change,False)
        button.connect("notify::day",self.date_change,False)
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
     
        button=gtk.Button("New Food")
        self.window.my_focus=button
        button.connect("clicked", self.newfood_response)
        table.attach(button,0,2,r,r+2)
        button.show()
        button=gtk.Button("Save")
        button.connect("clicked", self.save_response)
        table.attach(button,2,3,r,r+2)
        button.show()
        r=r+2


        self.window.add(table)
        table.show()
        self.window.show()
        self.optionsDialog = OptionsDialog(self)
        self.weightDialog = WeightList(self)
        self.weightDialog.build_run(self.window)
        self.paDialog=PAList(self)
        self.paDialog.build_run(self.window)
        self.foodDialog=FoodList(self)
        self.foodDialog.build_run(self.window)

        self.load()
        if self.optionsDialog.is_new:
            AboutDialog()
            self.optionsDialog.run(self.window)
            
        self.window.set_focus(self.window.my_focus)
    def load(self):
        self.today()
        self.draw()

    def run(self):
        gtk.main()

def main():
    app = FitnessApp()
    app.run()

if __name__=='__main__': 
    main()