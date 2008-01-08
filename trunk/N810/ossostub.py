class Context(object):
    def __init__(self,name,version,flag):
        self.name=name
        self.version=version
        self.flag=flag
        
class Autosave(object):
    def __init__(self,context):
        self.context=context
        self.cb=None
    def set_autosave_callback(self,cb,data=None):
        self.cb=cb
    def userdata_changed(self):
        self.force_autosave()
    def force_autosave(self):
        self.cb()
class OSSOStub:
    def __init__(self):
        self.Context = Context
        self.Autosave = Autosave


osso = OSSOStub()