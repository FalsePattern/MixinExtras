package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class ModifyReturnValueInjector extends Injector {
    public ModifyReturnValueInjector(InjectionInfo info) {
        super(info, "@ModifyReturnValue");
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        int opcode = node.getCurrentTarget().getOpcode();
        if (opcode < Opcodes.IRETURN || opcode >= Opcodes.RETURN) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s annotation is targeting an invalid insn in %s in %s",
                    this.annotationType, target, this));
        }
        this.checkTargetModifiers(target, false);
        this.injectReturnValueModifier(target, node);
    }

    private void injectReturnValueModifier(Target target, InjectionNode node) {
        InjectorData handler = new InjectorData(target, "return value modifier");
        InsnList insns = new InsnList();

        this.validateParams(handler, target.returnType, target.returnType);

        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            if (target.returnType.getSize() == 2) {
                insns.add(new InsnNode(Opcodes.DUP_X2));
                insns.add(new InsnNode(Opcodes.POP));
            } else {
                insns.add(new InsnNode(Opcodes.SWAP));
            }
        }

        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, handler.captureTargetArgs);
        }

        this.invokeHandler(insns);
        target.insertBefore(node, insns);
    }
}
