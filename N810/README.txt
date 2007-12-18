Fitness Record Book (python) 17-Dec-2007
========================================
This is the Python/Gtk version of the fitness-record-book.
The original version was designed for Palm and is described at
http://udi.benreuven.com/diet/

The program is optimized for usage on Nokia's Intenet Tablet (N800, N810)
running OS2008.

Installation
============
You have to agree to the license below before using this software.

OS2008
------
Install http://pymaemo.garage.maemo.org/installation.html
copy fitness.py to your device and run it. (don't copy hildon.py)

other Python environments
-------------------------
Install PyGtk (http://www.pygtk.org/downloads.html)
copy both fitness.py and hildon.py

Usage
=====
Identical to the original Palm version described at
http://udi.benreuven.com/diet/
with the following exceptions:

Save menu
---------
There is a menu option to store the application state to disk, otherwise all
the data you entered will be lost once you exit the program. The last stored
saved state is loaded whenever the application starts.

CSV files
---------
The application state is stored in several CSV files that have the name
fitness_xxx.csv  these files are located in the directory from which you run
the application.

You can copy these files to your PC for backup and even edit
them with Excel.

Each row in each CSV file holds the content of one dialog box in the applications
the columns in each row from left to right are the same as the entry fields
in the dialog box from top to bottom.

The csv files are:
* fitness_options - stores the content of the Options dialog box. There is just
  one options dialog box and there is just one line in this CSV file.
* fitness_weight,fitness_pas,fitness_foods - stores the weight, PA, food records.

Removed features
----------------
Some GUI features in the original palm version that were not useful were removed.

License
=======
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
