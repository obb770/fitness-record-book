#
# Create a debian package
# Depends on 'tar', ('gzip'), 'rm' and 'du'
#
# FIXME:
# - md5sums in control
#
from os import stat, mkdir, makedirs, chdir, system, chmod, popen
from os.path import join, basename, dirname
from base64 import b64encode
from StringIO import StringIO

name = "fitness"
version = "0.1.0-1"
arch = "all"
size = "0"

pyfiles = ["fitness.py", "lists.py", "dialogs.py", "items.py"]

control = """Package: %s
Version: %s
Section: user/tools
Priority: optional
Architecture: %s
Installed-Size: %s 
Maintainer: obb770 <obb770@gmail.com>
Depends: python2.5-runtime, hildon-application-manager
Description: Fitness record book
 Calorie counter application for the Internet Tablet
 .
"""

deb = "%s_%s_%s" % (name, version, arch)

def copy(src, open_src, dst, open_dst):
    if open_src:
        src = file(src, "rb")
    if open_dst:
        dst = file(dst, "wb")
    while True:
        buf = src.read(4096)
        if not buf:
            break
        dst.write(buf)
    if open_dst:
        dst.close()
    if open_src:
        src.close()

def archive(arname, files):
    arfile = file(arname, "wb")
    arfile.write("!<arch>\n");
    for filename in files:
        st = stat(filename)
        arfile.write("%-16s%-12d0     0     100644  %-10d\140\n" % 
                     (filename, st.st_mtime, st.st_size))
        copy(filename, True, arfile, False)
        if st.st_size % 2 != 0:
            arfile.write("\n")
    arfile.close()

def set_mode(filename, executable=False):
    import stat
    mode = stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH
    if executable:
        mode = mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH
    chmod(filename, mode)

def install(src, dst, executable=False):
    dstdir = dirname(dst)
    try:
        makedirs(dstdir)
    except:
        pass
    copy(src, False, dst, True)
    set_mode(dst, executable)
    

system("rm -rf %s.deb %s" % (deb, deb))
mkdir(deb)

# data
install(file(name, "rb"), join(deb, "data", "usr", "bin", name), True)

pkg_dir = join(deb, "data", "usr", "lib", "python2.5", "site-packages", name)
install(StringIO(""), join(pkg_dir, "__init__.py"))
for pyfile in pyfiles:
    install(file(pyfile, "rb"), join(pkg_dir, pyfile))

icon_dir = join(deb, "data", "usr", "share", "icons", "hicolor")
install(file(name + "_26x26.png", "rb"),
        join(icon_dir, "26x26", "hildon", name + ".png"))
install(file(name + "_40x40.png", "rb"),
        join(icon_dir, "40x40", "hildon", name + ".png"))
install(file(name + "_64x64.png", "rb"),
        join(icon_dir, "scalable", "hildon", name + ".png"))
install(file(name + ".desktop", "rb"),
        join(deb, "data", "usr", "share", "applications", "hildon", 
             name + ".desktop"))
install(file("README.txt", "rb"),
        join(deb, "data", "usr", "share", "doc", name, "copyright"))

system("tar zcfC " + join(deb, "data.tar.gz") + " " 
       + join(deb, "data") + " .")


# control
mkdir(join(deb, "control"))

# size
f = popen("du -k -s " + join(deb, "data"))
size = f.readline()
f.close()
l = size.find("\t")
if l > 0:
    size = size[:l]

# icon
f = file(join(icon_dir, "26x26", "hildon", name + ".png"), "rb")
icon = f.read()
f.close()
icon = b64encode(icon)
icon_field = "Maemo-Icon-26:\n "
icon_chars = []
count = 0
for c in icon:
    if count >= 69:
        icon_chars.append("\n")
        icon_chars.append(" ")
        count = 0
    icon_chars.append(c)
    count += 1
icon_chars.append("\n")

install(StringIO(control % (name, version, arch, size) + 
                 icon_field + "".join(icon_chars)), 
        join(deb, "control", "control"))

install(StringIO("""#!/bin/sh
gtk-update-icon-cache -f /usr/share/icons/hicolor
if [ "$1" = "configure" -a "$2" = "" ]; then
  maemo-select-menu-location fitness.desktop tana_fi_extras
fi
"""), join(deb, "control", "postinst"), True)

install(StringIO("""#!/bin/sh
rm -f /usr/lib/python2.5/site-packages/%s/*.pyo
""" 
        % (name,)), join(deb, "control", "prerm"), True)

system("tar zcfC " + join(deb, "control.tar.gz") + " " 
       + join(deb, "control") + " .")


install(StringIO("2.0\n"), join(deb, "debian-binary"));

chdir(deb)
archive(join("..",deb + ".deb"), 
        ("debian-binary", "control.tar.gz", "data.tar.gz"))
chdir("..")

system("rm -rf %s" % (deb,))

