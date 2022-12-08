package engine_modules_helpers;

public class ModuleIO {
    public IOType ioType;
    public IOData ioData;

    public ModuleIO(IOType ioType, IOData ioData) {
        this.ioType = ioType;
        this.ioData = ioData;
    }

    public ModuleIO() {
        this.ioType = IOType.None;
        this.ioData = null;
    }
}
