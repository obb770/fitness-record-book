#
# Create a debian package
#
from sys import argv
from os import mkdir, makedirs, chdir, chmod, getcwd, walk, remove, rmdir, \
    environ, popen
from os.path import getmtime, getsize, join, basename, dirname
from base64 import b64encode
from StringIO import StringIO

name = "fitness"
serviceprefix = "com.googlecode.FitnessRecordBook"
arch = "all"
size = "0"

pyfiles = ["fitness.py", "lists.py", "dialogs.py", "items.py"]

control = """Package: %s
Version: %s
Section: user/tools
Priority: optional
Architecture: %s
Installed-Size: %s 
Maintainer: obb 770 <obb770@gmail.com>
Depends: python2.5, python2.5-runtime, maemo-select-menu-location
Description: Calorie counter application for the Internet Tablet
 Manage your diet by keeping account of food and 
 physical activity.
 .
 Web site: http://benreuven.com/udi/diet
Maemo-Icon-26:
 %s
"""

servicefilename = "%s.%s.service" % (serviceprefix, name)
servicefile = """[D-BUS Service]
Name=%s.%s
Exec=/usr/bin/%s
""" % (serviceprefix, name, name)

desktopfilename = "%s.desktop" % (name,)
desktopfile = """[Desktop Entry]
Encoding=UTF-8
Version=1.0
Type=Application
Name=Fitness
Exec=/usr/bin/%s
Icon=%s
StartupWMClass=%s
X-Window-Icon=%s
X-Window-Icon-Dimmed=%s
X-Osso-Service=%s.%s
X-Osso-Type=application/x-executable
""" % (name, name, name, name, name, serviceprefix, name)

# version
if len(argv) > 1:
    version = argv[1]
elif "FITNESS_VERSION" in environ:
    version = environ["FITNESS_VERSION"]
else:
    f = popen("svn info | sed -n -e 's/Revision: *//p'", "rb")
    version = f.read()
    f.close()
    version = "1." + version[:-1]

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
        arfile.write("%-16s%-12d0     0     100644  %-10d\140\n" % 
                     (filename, getmtime(filename), getsize(filename)))
        copy(filename, True, arfile, False)
        if getsize(filename) % 2 != 0:
            arfile.write("\n")
    arfile.close()

def set_mode(filename, executable=False):
    import stat
    mode = stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH
    mode = mode | stat.S_IWUSR
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
    src.close()
    set_mode(dst, executable)
    
def py_install(src, datadir, dst):
    # from the py_compile.compile()
    from time import time
    mtime = time()
    code = ""
    if (src):
        mtime = getmtime(src)
        f = file(src, "U")
        code = f.read()
        f.close()
        if code and code[-1] != "\n":
            code += "\n"
    mtime = int(mtime)
    f = StringIO()
    from imp import get_magic
    f.write(get_magic())
    f.write(chr(mtime & 0xff))
    f.write(chr((mtime >> 8) & 0xff))
    f.write(chr((mtime >> 16) & 0xff))
    f.write(chr((mtime >> 24) & 0xff))
    from marshal import dumps
    f.write(dumps(compile(code, "/" + dst, "exec")))
    f.seek(0, 0)
    install(f, join(datadir, dst + "o"))

def tar(tarname, root):
    import tarfile
    tf = tarfile.open(tarname, "w:gz")
    cwd = getcwd()
    chdir(root)

    def add(path):
        ti = tf.gettarinfo(path)
        ti.uid = 0
        ti.uname = "root"
        ti.gid = 0
        ti.gname = "root"
        # TarInfo.tobuf() normalizes the path and removes the initial "./"
        # this causes the "linda" tool to fail.
        # Add "./" by intercepting the method and fixing the tar buffer
        tobuf = ti.tobuf
        def mytobuf(posix=False):
            buf = tobuf(posix)
            if not buf.startswith("./"):
                if len(ti.name) > 98:
                    raise Exception(
                        "tar: path length must be shorter than 98 chars")
                buf = "./" + buf[:(98 - 512)] + buf[(100 - 512):(148 - 512)] + \
                      "        " + buf[(156 - 512):]
                chksum = tarfile.calc_chksums(buf)[0]
                buf = buf[:-364] + "%06o\0" % chksum + buf[-357:]
                ti.buf = buf
            return buf
        ti.tobuf = mytobuf
        if ti.isreg():
            f = file(path, "rb")
            tf.addfile(ti, f)
            f.close()
        else:
            tf.addfile(ti)

    for top, dirs, files in walk("."):
        add(top)
        for f in files:
            add(join(top, f))
    chdir(cwd)
    tf.close()

def rm(path):
    try:
        remove(path)
        return
    except:
        pass
    try:
        for top, dirs, files in walk(path, topdown=False):
            for f in files:
                remove(join(top, f))
            for d in dirs:
                rmdir(join(top, d))
        rmdir(path)
    except:
        pass

def du(path):
    size = 0
    for top, dirs, files in walk(path):
        for f in files:
            size += getsize(join(top, f))
    return (size + 1023) // 1024

def md5sum(path, md5file):
    import md5
    cwd = getcwd()
    chdir(path)
    for top, dirs, files in walk("."):
        for name in files:
            f = file(join(top, name), "rb")
            m = md5.new()
            while True:
                buf = f.read(4096)
                if not buf:
                    break
                m.update(buf)
            f.close()
            md5file.write("%s  %s\n" % (m.hexdigest(), join(top, name)[2:]))
    chdir(cwd)

rm(deb + '.deb')
rm(deb)
mkdir(deb)

# data

# set the version and service name
f = file(name, "rb")
s = StringIO()
for line in f:
    if line.startswith('version = "<unknown>"'):
        s.write('version = "%s"\n' % (version,))
    elif line.startswith('service = "fitness"'):
        s.write('service = "%s.%s"\n' % (serviceprefix, name))
    else:
        s.write(line)
s.seek(0)
f.close()
install(s, join(deb, "data", "usr", "bin", name), True)

pkg_dir = join("usr", "lib", "python2.5", "site-packages", name)
py_install(None, join(deb, "data"), join(pkg_dir, "__init__.py"))
for pyfile in pyfiles:
    py_install(pyfile, join(deb, "data"), join(pkg_dir, pyfile))

icon_dir = join(deb, "data", "usr", "share", "icons", "hicolor")
install(file(name + "_26x26.png", "rb"),
        join(icon_dir, "26x26", "hildon", name + ".png"))
install(file(name + "_40x40.png", "rb"),
        join(icon_dir, "40x40", "hildon", name + ".png"))
install(file(name + "_64x64.png", "rb"),
        join(icon_dir, "scalable", "hildon", name + ".png"))
install(StringIO(desktopfile),
        join(deb, "data", "usr", "share", "applications", "hildon", 
             desktopfilename))
install(StringIO(servicefile),
        join(deb, "data", "usr", "share", "dbus-1", "services", 
             servicefilename))
install(file("README.txt", "rb"),
        join(deb, "data", "usr", "share", "doc", name, "copyright"))

chdir(deb)
tar("data.tar.gz", "data")
chdir("..")


# control
mkdir(join(deb, "control"))

# size
size = "%d" % (du(join(deb, "data")),)

# icon
f = file(join(icon_dir, "26x26", "hildon", name + ".png"), "rb")
icon = f.read()
f.close()
icon_chars = []
for i, c in enumerate(b64encode(icon)):
    icon_chars.append(c)
    if (i + 1) % 69 == 0:
        icon_chars.append("\n")
        icon_chars.append(" ")
icon = "".join(icon_chars)

install(StringIO(control % (name, version, arch, size, icon)),
        join(deb, "control", "control"))

md5sums = join(deb, "control", "md5sums")
md5file =  file(md5sums, "wb")
md5sum(join(deb, "data"), md5file)
md5file.close()
set_mode(md5sums)

install(StringIO("""#!/bin/sh
gtk-update-icon-cache -f /usr/share/icons/hicolor
if [ "$1" = "configure" -a "$2" = "" ]; then
  maemo-select-menu-location %s.desktop tana_fi_extras
fi
""" % (name,)), join(deb, "control", "postinst"), True)

chdir(deb)
tar("control.tar.gz", "control")
chdir("..")

install(StringIO("2.0\n"), join(deb, "debian-binary"));

chdir(deb)
archive(join("..",deb + ".deb"), 
        ("debian-binary", "control.tar.gz", "data.tar.gz"))
chdir("..")

rm(deb)

