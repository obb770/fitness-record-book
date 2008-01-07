#!/usr/bin/env python2.5
# TODO optimize button size for Nokia's screen
# TODO package
# TODO remove old records in DateObjList accoring to OptionsDialog.history
# TODO add to OptionsDialog the folder location for CSV files
# TODO DateObj.run.Cancel
# TODO DateObj.run.Del
# TODO validate date range
# TODO mark the location of input errors in dialog
# TODO localization

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
try:
    import osso
except:
    from ossostub import osso
try:
    import hildon
except:
    from hildonstub import hildon

# the application main window launches lists which in tern launches
# dialogs which in turn are made from items
from items import *
from dialogs import *
from lists import *


class AboutDialog(gtk.AboutDialog):
    def __init__(self):
        gtk.AboutDialog.__init__(self)
        self.set_size_request(*SZ)
        self.set_name("Fitness Record Book (Python/Gtk)")
        self.set_version("0.1")
        self.set_website("http://benreuven.com/udiwiki/index.php?title=Fitness_Record_Book")
        self.set_comments("You must agree to the license before using this program")
        self.set_authors(["Eric W. Sink","Ehud (Udi) Ben-Reuven"])
        self.set_copyright("""Copyright 1997 Eric W. Sink; Copyright 2000-4,2007 Ehud (Udi) Ben-Reuven""")
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
    def save(self, user_data=None):
        self.optionsDialog.save()
        self.foodDialog.save()
        self.paDialog.save()
        self.weightDialog.save()
    def menuitem_response(self, widget, data):
        if data==0:
            self.optionsDialog.run(self.window)
            self.draw()
        elif data==1:
            self.force_save()
        else:
            AboutDialog()

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
    def force_save(self):
        self.autosave.force_autosave()
    def quit(self, evt):
        self.force_save()
        gtk.main_quit()
    def __init__(self):
        self.sdate=Date(2007,12,1)
        self.edate=Date(2007,12,1)

        hildon.Program.__init__(self)
        self.context = osso.Context("fitness", "0.0.1", False)
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
        for l in ["Today","Yesterday","This Week"]:
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
        self.optionsDialog = OptionsDialog(self)
        self.weightDialog = WeightList(self)
        self.paDialog=PAList(self)
        self.foodDialog=FoodList(self)

        self.load()
        if self.optionsDialog.is_new:
            AboutDialog()
            self.optionsDialog.run(self.window)
    def load(self):
        self.today()
        self.draw()

    def run(self):
        gtk.main()

app = FitnessApp()
app.run()
