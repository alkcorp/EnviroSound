package com.sonicether.soundphysics;

import java.util.Iterator;
import java.util.ListIterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;

public class CoreModInjector implements IClassTransformer {

	@Override
	public byte[] transform(final String obfuscated, final String deobfuscated, byte[] bytes) {
		if (obfuscated.equals("chk$a")) {
			// Inside SoundManager.SoundSystemStarterThread
			InsnList toInject = new InsnList();
			toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/sonicether/soundphysics/SoundPhysics", "init",
					"()V", false));

			// Target method: Constructor
			bytes = patchMethodInClass(obfuscated, bytes, "<init>", "(Lchk;)V", Opcodes.INVOKESPECIAL,
					AbstractInsnNode.METHOD_INSN, "<init>", null, toInject, false, 0, 0, false, 0);
		} else

		if (obfuscated.equals("chk")) {
			// Inside SoundManager
			InsnList toInject = new InsnList();

			toInject.add(new VarInsnNode(Opcodes.ALOAD, 7));
			toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/sonicether/soundphysics/SoundPhysics",
					"setLastSoundCategory", "(Lqe;)V", false));

			// Target method: playSound
			bytes = patchMethodInClass(obfuscated, bytes, "c", "(Lcgr;)V", Opcodes.INVOKEVIRTUAL,
					AbstractInsnNode.METHOD_INSN, "setVolume", null, toInject, false, 0, 0, false, 0);

			toInject = new InsnList();
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));
			toInject.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "cgr", "a", "()Lnd;", true));
			toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "nd", "toString", "()Ljava/lang/String;", false));
			toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/sonicether/soundphysics/SoundPhysics",
					"setLastSoundName", "(Ljava/lang/String;)V", false));

			// Target method: playSound
			bytes = patchMethodInClass(obfuscated, bytes, "c", "(Lcgr;)V", Opcodes.INVOKEVIRTUAL,
					AbstractInsnNode.METHOD_INSN, "setVolume", null, toInject, false, 0, 0, false, 0);

			toInject = new InsnList();
			toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "com/sonicether/soundphysics/SoundPhysics",
					"globalVolumeMultiplier", "F"));
			toInject.add(new InsnNode(Opcodes.FMUL));

			// Target method: playSound
			bytes = patchMethodInClass(obfuscated, bytes, "c", "(Lcgr;)V", Opcodes.INVOKESPECIAL,
					AbstractInsnNode.METHOD_INSN, "e", "(Lcgr;)F", toInject, false, 0, 0, false, 0);
		} else

		if (obfuscated.equals("paulscode.sound.libraries.SourceLWJGLOpenAL")) {
			// Inside SourceLWJGLOpenAL
			InsnList toInject = new InsnList();

			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInject.add(new FieldInsnNode(Opcodes.GETFIELD, "paulscode/sound/libraries/SourceLWJGLOpenAL", "position",
					"Lpaulscode/sound/Vector3D;"));
			toInject.add(new FieldInsnNode(Opcodes.GETFIELD, "paulscode/sound/Vector3D", "x", "F"));
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInject.add(new FieldInsnNode(Opcodes.GETFIELD, "paulscode/sound/libraries/SourceLWJGLOpenAL", "position",
					"Lpaulscode/sound/Vector3D;"));
			toInject.add(new FieldInsnNode(Opcodes.GETFIELD, "paulscode/sound/Vector3D", "y", "F"));
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInject.add(new FieldInsnNode(Opcodes.GETFIELD, "paulscode/sound/libraries/SourceLWJGLOpenAL", "position",
					"Lpaulscode/sound/Vector3D;"));
			toInject.add(new FieldInsnNode(Opcodes.GETFIELD, "paulscode/sound/Vector3D", "z", "F"));
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInject.add(new FieldInsnNode(Opcodes.GETFIELD, "paulscode/sound/libraries/SourceLWJGLOpenAL",
					"channelOpenAL", "Lpaulscode/sound/libraries/ChannelLWJGLOpenAL;"));
			toInject.add(new FieldInsnNode(Opcodes.GETFIELD, "paulscode/sound/libraries/ChannelLWJGLOpenAL", "ALSource",
					"Ljava/nio/IntBuffer;"));
			toInject.add(new InsnNode(Opcodes.ICONST_0));
			toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/IntBuffer", "get", "(I)I", false));
			toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/sonicether/soundphysics/SoundPhysics",
					"onPlaySound", "(FFFI)V", false));

			// Target method: play
			bytes = patchMethodInClass(obfuscated, bytes, "play", "(Lpaulscode/sound/Channel;)V", Opcodes.INVOKEVIRTUAL,
					AbstractInsnNode.METHOD_INSN, "play", null, toInject, false, 0, 0, false, 0);
		} else

		if (obfuscated.equals("paulscode.sound.SoundSystem")) {
			// Inside SoundSystem
			InsnList toInject = new InsnList();

			toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "com/sonicether/soundphysics/SoundPhysics",
					"globalVolumeMultiplier", "F"));
			toInject.add(new InsnNode(Opcodes.FMUL));

			// Target method: newSource
			bytes = patchMethodInClass(obfuscated, bytes, "newSource",
					"(ZLjava/lang/String;Ljava/net/URL;Ljava/lang/String;ZFFFIF)V", Opcodes.INVOKESPECIAL,
					AbstractInsnNode.METHOD_INSN, "<init>", null, toInject, true, 0, 0, false, 0);
		} else

		if (obfuscated.equals("pj")) {
			// Inside PlayerList
			InsnList toInject = new InsnList();

			// Multiply sound distance volume play decision by
			// SoundPhysics.soundDistanceAllowance
			toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "com/sonicether/soundphysics/SoundPhysics",
					"soundDistanceAllowance", "D"));
			toInject.add(new InsnNode(Opcodes.DMUL));

			// Target method: sendToAllNearExcept
			bytes = patchMethodInClass(obfuscated, bytes, "a", "(Laeb;DDDDILht;)V", Opcodes.DCMPG,
					AbstractInsnNode.INSN, "", "", toInject, true, 0, 0, false, 0);
		} else

		if (obfuscated.equals("ve")) {
			// Inside Entity
			InsnList toInject = new InsnList();

			// Offset entity sound by their eye height
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));
			toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/sonicether/soundphysics/SoundPhysics",
					"calculateEntitySoundOffset", "(Lve;Lqc;)D", false));
			toInject.add(new InsnNode(Opcodes.DADD));
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));

			// Target method: playSound
			// Inside target method, target node: Entity/getSoundCategory
			bytes = patchMethodInClass(obfuscated, bytes, "a", "(Lqc;FF)V", Opcodes.INVOKEVIRTUAL,
					AbstractInsnNode.METHOD_INSN, "bK", null, toInject, true, 0, 0, false, -3);
		}

		return bytes;
	}

	private byte[] patchMethodInClass(String className, final byte[] bytes, final String targetMethod,
			final String targetMethodSignature, final int targetNodeOpcode, final int targetNodeType,
			final String targetInvocationMethodName, final String targetInvocationMethodSignature,
			final InsnList instructionsToInject, final boolean insertBefore, final int nodesToDeleteBefore,
			final int nodesToDeleteAfter, final boolean deleteTargetNode, final int targetNodeOffset) {
		log("//// Patching Class: " + className);

		// Setup ASM class manipulation stuff
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		// Now we loop over all of the methods declared inside the class until
		// we get to the target method name
		final Iterator<MethodNode> methodIterator = classNode.methods.iterator();
		while (methodIterator.hasNext()) {
			final MethodNode m = methodIterator.next();
			log("@" + m.name + " " + m.desc);

			// Check if this is the method name and the signature matches
			if (m.name.equals(targetMethod) && m.desc.equals(targetMethodSignature)) {
				log("Inside target method: " + targetMethod);

				AbstractInsnNode targetNode = null;

				// Loop over the instruction set
				final ListIterator<AbstractInsnNode> nodeIterator = m.instructions.iterator();
				while (nodeIterator.hasNext()) {
					AbstractInsnNode currentNode = nodeIterator.next();

					if (currentNode.getOpcode() == targetNodeOpcode) {

						if (targetNodeType == AbstractInsnNode.METHOD_INSN) {
							if (currentNode.getType() == AbstractInsnNode.METHOD_INSN) {
								final MethodInsnNode method = (MethodInsnNode) currentNode;
								if (method.name.equals(targetInvocationMethodName)) {
									if (method.desc.equals(targetInvocationMethodSignature)
											|| targetInvocationMethodSignature == null) {
										log("Found target method invocation for injection: "
												+ targetInvocationMethodName);
										targetNode = currentNode;
										// Due to collisions, do not put break
										// statements here!
									}

								}
							}
						} else {
							if (currentNode.getType() == targetNodeType) {
								log("Found target node for injection: " + targetNodeOpcode);
								targetNode = currentNode;
								// Due to collisions, do not put break
								// statements here!
							}
						}

					}
				}

				if (targetNode == null) {
					SoundPhysics.logError("Target node not found!" + className);
					break;
				}

				// Offset the target node by the supplied offset value
				if (targetNodeOffset > 0) {
					for (int i = 0; i < targetNodeOffset; i++) {
						targetNode = targetNode.getNext();
					}
				} else if (targetNodeOffset < 0) {
					for (int i = 0; i < -targetNodeOffset; i++) {
						targetNode = targetNode.getPrevious();
					}
				}

				// If we've found the target, inject the instructions!
				for (int i = 0; i < nodesToDeleteBefore; i++) {
					final AbstractInsnNode previousNode = targetNode.getPrevious();
					log("Removing Node " + previousNode.getOpcode());
					m.instructions.remove(previousNode);
				}

				for (int i = 0; i < nodesToDeleteAfter; i++) {
					final AbstractInsnNode nextNode = targetNode.getNext();
					log("Removing Node " + nextNode.getOpcode());
					m.instructions.remove(nextNode);
				}

				if (insertBefore) {
					m.instructions.insertBefore(targetNode, instructionsToInject);
				} else {
					m.instructions.insert(targetNode, instructionsToInject);
				}

				if (deleteTargetNode) {
					m.instructions.remove(targetNode);
				}

				break;
			}
		}

		log("//// Class finished: " + className);

		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private void log(final String message) {
		if (Config.debugLogging) {
			SoundPhysics.log(message);
		}
	}

}
