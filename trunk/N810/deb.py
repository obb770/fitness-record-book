#
# Create a debian package
# Depends on 'tar', ('gzip'), 'rm' and 'du'
#
# FIXME:
# - md5sums in control
#
from os import stat, mkdir, makedirs, chdir, system, chmod, popen
from os.path import split, join, basename, dirname
from base64 import b64encode

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

def write_file(filename, contents, executable=False):
    try:
        makedirs(dirname(filename))
    except:
        pass
    f = file(filename, "wb")
    f.write(contents)
    f.close()
    set_mode(filename, executable)

def install(src, dst, executable=False):
    (dstdir, dstbase) = split(dst)
    try:
        makedirs(dstdir)
    except:
        pass
    if dstbase == "":
        dst = join(dstdir, basename(src))
    copy(src, True, dst, True)
    set_mode(dst, executable)
    

system("rm -rf %s.deb %s" % (deb, deb))
mkdir(deb)

# data
install(name, join(deb, "data", "usr", "bin", name), True)

pkg_dir = join(deb, "data", "usr", "lib", "python2.5", "site-packages", name)
write_file(join(pkg_dir, "__init__.py"), "")
for pyfile in pyfiles:
    install(pyfile, join(pkg_dir, ""))

icon_dir = join(deb, "data", "usr", "share", "icons", "hicolor")
install(name + "_26x26.png", join(icon_dir, "26x26", "hildon", name + ".png"))
install(name + "_40x40.png", join(icon_dir, "40x40", "hildon", name + ".png"))
install(name + "_64x64.png",
        join(icon_dir, "scalable", "hildon", name + ".png"))
install(name + ".desktop", 
        join(deb, "data", "usr", "share", "applications", "hildon", ""))
install("README.txt", 
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

write_file(join(deb, "control", "control"), 
           control % (name, version, arch, size) + 
           icon_field + "".join(icon_chars))

write_file(join(deb, "control", "postinst"), 
           """#!/bin/sh
gtk-update-icon-cache -f /usr/share/icons/hicolor
if [ "$2" = "" ]; then
  maemo-select-menu-location fitness.desktop
fi
""", 
           True)

write_file(join(deb, "control", "prerm"), 
           "#!/bin/sh\nrm -f /usr/lib/python2.5/site-packages/" + name + 
           "/*.pyo\n",
           True)

system("tar zcfC " + join(deb, "control.tar.gz") + " " 
       + join(deb, "control") + " .")


write_file(join(deb, "debian-binary"), "2.0\n")

chdir(deb)
archive(join("..",deb + ".deb"), 
        ("debian-binary", "control.tar.gz", "data.tar.gz"))
chdir("..")

system("rm -rf %s" % (deb,))

