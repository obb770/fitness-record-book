Fitness Record Book (python) 17-Dec-2007
========================================
This is the Python/Gtk version of the fitness-record-book.
The program is optimized for usage on Nokia's Intenet Tablet (N800, N810)
However it can be run on other python environments.

Installation
============
You have to agree to the license below before using this software.

On N8xx
-------
Install http://pymaemo.garage.maemo.org/
copy fitness.py to your device and run it. (don't copy hildon.py)

On other Python environments
----------------------------
Install pygtk
copy both fitness.py and hildon.py

Usage
=====
Identical to http://udi.benreuven.com/diet/ with the following exceptions:
* There is a menu option to store the application state to disk, otherwise all
  the data you entered will be lost once you exit the program. The last stored
  saved state is loaded when the application starts.
* The application state is stored in several csv files that have the name
  fitness_xxx.csv  these files are located in the directory from which you run
  the application. You can copy these files to your PC for backup and even edit
  them with excel.
* some GUI features in the original version that were not useful were removed.

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
