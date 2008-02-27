#!/usr/bin/env python

#
# This is a quick hack to enable uploading and deleting files on your
# googlecode project. It borrows from googlecode_upload.py and from libgmail
# for details on how to upload files and obtain the SID cookie by signing in
# to google accounts.
# The script probes the Subversion configuration files for authentication
# information and caches the google account SID cookie in the current
# working directory.
#
from urllib import urlencode
from urllib2 import urlopen, Request, HTTPError
from  base64 import b64encode
from sys import argv, stdin, stdout
from getpass import getpass
from re import match
from os import listdir, remove, getenv
from os.path import join, expanduser, exists, basename

def getcred(userprompt, defaultuser=None):
    defaultstr = ""
    if defaultuser:
        defaultstr = " [%s]" % (defaultuser,)
    stdout.write("%s%s: " % (userprompt, defaultstr))
    stdout.flush()
    user = stdin.readline().rstrip()
    if user == "":
        user = defaultuser
    password = getpass()
    return user, password

def getsvn(project, force=True):
    svndir = getenv("SUBVERSION_DIR", None)
    if not svndir:
        svndir = expanduser('~/.subversion')
    authdir = join(svndir, "auth", "svn.simple")
    user, password = None, None
    if exists(authdir):
        for hash in listdir(authdir):
            f = file(join(authdir, hash))
            lines = f.read().splitlines()
            credentials = {}
            for i in xrange(0, len(lines) - 1, 4):
                credentials[lines[i + 1]] = lines[i + 3]
            f.close()
            if credentials['svn:realmstring'].find(project) >= 0:
                user = credentials['username']
                password = credentials['password']
                break
    if not user and force:
        return getcred("Subversion user")
    return user, password

def getsid(defaultuser=None, force_login=False):
    SIDFILE = "googlesid"
    if exists(SIDFILE):
        if not force_login:
            f = file(SIDFILE)
            sid = f.readline().rstrip()
            f.close()
            return sid
        else:
            remove(SIDFILE)
    user, password = getcred("Gmail user", defaultuser)
    url = "https://www.google.com/accounts/ServiceLoginBoxAuth"
    page = urlopen(url, urlencode((("Email", user), ("Passwd", password))))
    for cookie in page.info().getheader("set-cookie").split(","):
        if cookie.startswith("SID="):
            sid = cookie.split(";")[0]
            break
    else:
        he = HTTPError(url, 400, "Bad credentials", None, None)
        he.url = url
        raise he
    f = file(SIDFILE, "wb")
    f.write(sid)
    f.close()
    return sid

def delete_do(project, filename, force_login=False):
    sid = getsid(getsvn(project, False)[0], force_login)
    url = "http://code.google.com/p/%s/downloads/" % (project,)
    req = Request(url + "delete?filename=" + filename, headers={"Cookie": sid})
    page = " ".join(urlopen(req).read().splitlines())
    try:
        token = match(r'.*name=token\s+value="([^"]*)"', page).groups()[0]
        pagegen = match(r'.*name=pagegen\s+value="([^"]*)"', page).groups()[0]
    except:
        # bad SID ?
        if not force_login:
            return delete_do(project, filename, True)
        else:
            return 400, "Too many failures"
    req = Request(url + "delete.do", 
                  data=urlencode((("token", token), 
                                  ("pagegen", pagegen),
                                  ("filename", filename), 
                                  ("delete", "Delete Download"))),
                  headers={"Cookie": sid})
    urlopen(req)
    return 200, "Deleted"

def delete(project, filename):
    status, reason = None, None
    try:
        status, reason = delete_do(project, filename)
    except HTTPError, e:
        return e.code, e.msg, e.url
    return (status, reason, 
            "%s.googlecode.com/files/%s" % (project, filename))

def upload_do(filename, project, user, password, summary, labels):
    url = "https://%s.googlecode.com/files" % (project, )
    req = Request(url)
    BOUNDARY = "theboundary"
    req.add_header("Content-type", 
                   "multipart/form-data; boundary=%s" % BOUNDARY)
    req.add_header("Authorization", 
                   "Basic %s" % b64encode(":".join((user, password))))
    data = []
    fields = [("summary", summary)]
    if labels != None:
        fields.extend([("label", label) for label in labels])
    for name, value in fields:
        data.append("--" + BOUNDARY)
        data.append('Content-Disposition: form-data; name="%s"' % name)
        data.append("")
        data.append(value)
    data.append("--" + BOUNDARY)
    data.append('Content-Disposition: form-data; name="filename"' +
                '; filename="%s"' % basename(filename))
    data.append("")
    f = file(filename, "rb")
    data.append(f.read())
    f.close()
    data.append("--" + BOUNDARY + "--")
    data.append("")
    req.add_data("\r\n".join(data))
    location = None
    try:
       urlopen(req)
    except HTTPError, e:
        location = e.info().getheader("location")
        if not location:
            location = e.url
        return e.code, e.msg, location

def upload(project, filename, summary="", labels=None):
    user, password = getsvn(project)
    if not user or not password:
        return 400, "No svn credentials", project
    return upload_do(filename, project, user, password, summary, labels)

def main():
    result = (0, """
Usage: %s delete <project> <file-name>
       %s upload <project> <file-name> <summary> [<label>,<label>,...]
""" 
              % (argv[0], argv[0]), "")
    if len(argv) == 4 and argv[1] == "delete":
        result = delete(argv[2], argv[3])
    elif len(argv) >= 5 and argv[1] == "upload":
        labels = None
        if len(argv) >= 6:
            labels = argv[5].split(",")
        result = upload(argv[2], argv[3], argv[4], labels)
    print "%d %s %s" % result

if __name__ == "__main__":
    main()

