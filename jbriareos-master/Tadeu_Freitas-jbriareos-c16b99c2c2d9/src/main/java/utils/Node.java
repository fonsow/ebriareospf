package utils;

import core.JMSInterface;
import engine_modules.EngineModule;
import engine_modules_helpers.ModuleIO;
import engine_modules_helpers.ModuleType;

import java.util.UUID;

public class Node {
    public UUID nodeId;
    public EngineModule module;
    Common.InputMode inputMode;

    public Node(EngineModule module) {
        this.nodeId = UUID.randomUUID();
        this.module = module;
        this.inputMode = module.getInputMode();
    }

    public ModuleType getModType() {
        return this.module.getModType();
    }

    public Common.InputMode getModInputMode() {
        return this.module.getInputMode();
    }

    public ModuleIO process(BPacket packet, ModuleIO data, JMSInterface jmsInterface) {
        return this.module.process(packet, data, jmsInterface);
    }
}
