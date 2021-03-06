/*******************************************************************************
 *                     GNU GENERAL PUBLIC LICENSE
 *                        Version 3, 29 June 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 *
 *  https://github.com/sonicether/Sound-Physics/blob/master/LICENSE
 *
 * Copyright (c) 2017, 2019 AlkCorp.
 * Contributors: https://github.com/alkcorp/Sound-Physics/graphs/contributors
 *******************************************************************************/
package net.alkalus.envirosound;

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

	private static final String logPrefix = "[SOUND PHYSICS INJECTOR]";

	public static void log(final String message) {
		if (Config.injectorLogging) {
			System.out.println(
					CoreModInjector.logPrefix.concat(" : ").concat(message)
			);
		}
	}

	public static void logError(final String errorMessage) {
		System.out.println(
				CoreModInjector.logPrefix.concat(" [ERROR] : ").concat(errorMessage)
		);
	}

	private byte[] patchMethodInClass(
			final String className, final byte[] bytes, final String targetMethod, final String targetMethodSignature, final int targetNodeOpcode, final int targetNodeType, final String targetInvocationMethodName, final String targetInvocationMethodSignature, final int targetVarNodeIndex, final InsnList instructionsToInject, final boolean insertBefore, final int nodesToDeleteBefore, final int nodesToDeleteAfter, final boolean deleteTargetNode, final int targetNodeOffset
	) {
		CoreModInjector.log("Patching class : " + className);

		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		final Iterator<MethodNode> methodIterator = classNode.methods.iterator();

		while (methodIterator.hasNext()) {
			final MethodNode m = methodIterator.next();
			// log("@" + m.name + " " + m.desc);

			if (m.name.equals(targetMethod)
					&& m.desc.equals(targetMethodSignature)) {
				CoreModInjector.log("Inside target method: " + targetMethod);

				AbstractInsnNode targetNode = null;

				final ListIterator<AbstractInsnNode> nodeIterator = m.instructions.iterator();
				while (nodeIterator.hasNext()) {
					final AbstractInsnNode currentNode = nodeIterator.next();
					if (currentNode.getOpcode() == targetNodeOpcode) {

						if (targetNodeType == AbstractInsnNode.METHOD_INSN) {
							if (currentNode.getType() == AbstractInsnNode.METHOD_INSN) {
								final MethodInsnNode method = (MethodInsnNode) currentNode;
								if (method.name.equals(
										targetInvocationMethodName
								)) {
									if (method.desc.equals(
											targetInvocationMethodSignature
									) || (targetInvocationMethodSignature == null)) {
										CoreModInjector.log(
												"Found target method invocation for injection: "
														+ targetInvocationMethodName
										);
										targetNode = currentNode;
										// Due to collisions, do not put break
										// statements here!
									}

								}
							}
						}
						else if (targetNodeType == AbstractInsnNode.VAR_INSN) {
							if (currentNode.getType() == AbstractInsnNode.VAR_INSN) {
								final VarInsnNode varnode = (VarInsnNode) currentNode;
								if ((targetVarNodeIndex < 0)
										|| (varnode.var == targetVarNodeIndex)) {
									CoreModInjector.log(
											"Found target var node for injection: "
													+ targetVarNodeIndex
									);
									targetNode = currentNode;
									// Due to collisions, do not put break
									// statements here!
								}
							}
						}
						else {
							if (currentNode.getType() == targetNodeType) {
								CoreModInjector.log(
										"Found target node for injection: "
												+ targetNodeType
								);
								targetNode = currentNode;
								// Due to collisions, do not put break
								// statements here!
							}
						}

					}
				}

				if (targetNode == null) {
					CoreModInjector.logError(
							"Target node not found! " + className
					);
					break;
				}

				// Offset the target node by the supplied offset value
				if (targetNodeOffset > 0) {
					for (int i = 0; i < targetNodeOffset; i++) {
						targetNode = targetNode.getNext();
					}
				}
				else if (targetNodeOffset < 0) {
					for (int i = 0; i < -targetNodeOffset; i++) {
						targetNode = targetNode.getPrevious();
					}
				}

				// If we've found the target, inject the instructions!
				for (int i = 0; i < nodesToDeleteBefore; i++) {
					final AbstractInsnNode previousNode = targetNode.getPrevious();
					CoreModInjector.log(
							"Removing Node " + previousNode.getOpcode()
					);
					m.instructions.remove(previousNode);
				}

				for (int i = 0; i < nodesToDeleteAfter; i++) {
					final AbstractInsnNode nextNode = targetNode.getNext();
					CoreModInjector.log(
							"Removing Node " + nextNode.getOpcode()
					);
					m.instructions.remove(nextNode);
				}

				if (insertBefore) {
					m.instructions.insertBefore(
							targetNode, instructionsToInject
					);
				}
				else {
					m.instructions.insert(targetNode, instructionsToInject);
				}

				if (deleteTargetNode) {
					m.instructions.remove(targetNode);
				}

				break;
			}
		}
		CoreModInjector.log("Class finished : " + className);

		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	@Override
	public byte[] transform(
			final String obfuscated, final String deobfuscated, byte[] bytes
	) {
		if (obfuscated.equals("btn")) {
			// Inside SoundManager.SoundSystemStarterThread
			final InsnList toInject = new InsnList();
			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKESTATIC, "net/alkalus/envirosound/EnviroSound", "init", "()V", false
					)
			);

			// Target method: Constructor
			bytes = patchMethodInClass(
					obfuscated, bytes, "<init>", "(Lbtj;)V", Opcodes.INVOKESPECIAL, AbstractInsnNode.METHOD_INSN, "<init>", null, -1, toInject, false, 0, 0, false, 0
			);
		}
		else

		if (obfuscated.equals("btj")) {
			// Inside SoundManager
			InsnList toInject = new InsnList();
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 6));
			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKESTATIC, "net/alkalus/envirosound/EnviroSound", "setLastSoundCategory", "(Lw;)V", false
					)
			);

			// Target method: playSound
			bytes = patchMethodInClass(
					obfuscated, bytes, "c", "(Lbst;)V", Opcodes.INVOKEVIRTUAL, AbstractInsnNode.METHOD_INSN, "setVolume", null, -1, toInject, false, 0, 0, false, 0
			);

			toInject = new InsnList();
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 10));
			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKEVIRTUAL, "bqx", "toString", "()Ljava/lang/String;", false
					)
			);
			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKESTATIC, "net/alkalus/envirosound/EnviroSound", "setLastSoundName", "(Ljava/lang/String;)V", false
					)
			);

			// Target method: playSound
			bytes = patchMethodInClass(
					obfuscated, bytes, "c", "(Lbst;)V", Opcodes.INVOKEVIRTUAL, AbstractInsnNode.METHOD_INSN, "setVolume", null, -1, toInject, false, 0, 0, false, 0
			);

			toInject = new InsnList();
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETSTATIC, "net/alkalus/envirosound/EnviroSound", "globalVolumeMultiplier", "F"
					)
			);
			toInject.add(new InsnNode(Opcodes.FMUL));

			// Target method: playSound, target invocation getClampedVolume
			bytes = patchMethodInClass(
					obfuscated, bytes, "c", "(Lbst;)V", Opcodes.INVOKESPECIAL, AbstractInsnNode.METHOD_INSN, "a", "(Lbst;Lbti;Lw;)F", -1, toInject, false, 0, 0, false, 0
			);
		}
		else

		if (obfuscated.equals("paulscode.sound.libraries.SourceLWJGLOpenAL")) {
			// Inside SourceLWJGLOpenAL
			final InsnList toInject = new InsnList();

			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "paulscode/sound/libraries/SourceLWJGLOpenAL", "position", "Lpaulscode/sound/Vector3D;"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "paulscode/sound/Vector3D", "x", "F"
					)
			);
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "paulscode/sound/libraries/SourceLWJGLOpenAL", "position", "Lpaulscode/sound/Vector3D;"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "paulscode/sound/Vector3D", "y", "F"
					)
			);
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "paulscode/sound/libraries/SourceLWJGLOpenAL", "position", "Lpaulscode/sound/Vector3D;"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "paulscode/sound/Vector3D", "z", "F"
					)
			);
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "paulscode/sound/libraries/SourceLWJGLOpenAL", "channelOpenAL", "Lpaulscode/sound/libraries/ChannelLWJGLOpenAL;"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "paulscode/sound/libraries/ChannelLWJGLOpenAL", "ALSource", "Ljava/nio/IntBuffer;"
					)
			);
			toInject.add(new InsnNode(Opcodes.ICONST_0));
			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKEVIRTUAL, "java/nio/IntBuffer", "get", "(I)I", false
					)
			);
			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKESTATIC, "net/alkalus/envirosound/EnviroSound", "onPlaySound", "(FFFI)V", false
					)
			);

			// Target method: play
			bytes = patchMethodInClass(
					obfuscated, bytes, "play", "(Lpaulscode/sound/Channel;)V", Opcodes.INVOKEVIRTUAL, AbstractInsnNode.METHOD_INSN, "play", null, -1, toInject, false, 0, 0, false, 0
			);
		}
		else

		// Convert stero sounds to mono
		if (obfuscated.equals("paulscode.sound.libraries.LibraryLWJGLOpenAL")
				&& Config.autoSteroDownmix) {
			// Inside LibraryLWJGLOpenAL
			InsnList toInject = new InsnList();

			toInject.add(new VarInsnNode(Opcodes.ALOAD, 4));
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));
			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKEVIRTUAL, "paulscode/sound/FilenameURL", "getFilename", "()Ljava/lang/String;", false
					)
			);

			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKESTATIC, "net/alkalus/envirosound/EnviroSound", "onLoadSound", "(Lpaulscode/sound/SoundBuffer;Ljava/lang/String;)Lpaulscode/sound/SoundBuffer;", false
					)
			);

			toInject.add(new VarInsnNode(Opcodes.ASTORE, 4));
			// buffer =
			// onLoadSound(EnviroSound.buffer,filenameURL.getFilename());

			// Target method: loadSound
			bytes = patchMethodInClass(
					obfuscated, bytes, "loadSound", "(Lpaulscode/sound/FilenameURL;)Z", Opcodes.INVOKEINTERFACE, AbstractInsnNode.METHOD_INSN, "cleanup", null, -1, toInject, false, 0, 0, false, 0
			);

			toInject = new InsnList();

			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));

			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKESTATIC, "net/alkalus/envirosound/EnviroSound", "onLoadSound", "(Lpaulscode/sound/SoundBuffer;Ljava/lang/String;)Lpaulscode/sound/SoundBuffer;", false
					)
			);

			toInject.add(new VarInsnNode(Opcodes.ASTORE, 0));

			// Target method: loadSound
			bytes = patchMethodInClass(
					obfuscated, bytes, "loadSound", "(Lpaulscode/sound/SoundBuffer;Ljava/lang/String;)Z", Opcodes.INVOKEVIRTUAL, AbstractInsnNode.METHOD_INSN, "getChannels", null, -1, toInject, true, 0, 0, false, -12
			);
		}
		else

		if (obfuscated.equals("paulscode.sound.SoundSystem")) {
			// Inside SoundSystem
			final InsnList toInject = new InsnList();

			toInject.add(
					new FieldInsnNode(
							Opcodes.GETSTATIC, "net/alkalus/envirosound/EnviroSound", "attenuationModel", "I"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETSTATIC, "net/alkalus/envirosound/EnviroSound", "globalRolloffFactor", "F"
					)
			);

			// Target method: newSource
			bytes = patchMethodInClass(
					obfuscated, bytes, "newSource", "(ZLjava/lang/String;Ljava/net/URL;Ljava/lang/String;ZFFFIF)V", Opcodes.INVOKESPECIAL, AbstractInsnNode.METHOD_INSN, "<init>", null, -1, toInject, true, 2, 0, false, 0
			);
		}
		else

		if (obfuscated.equals("oi")) {
			// Inside ServerConfigurationManager
			final InsnList toInject = new InsnList();

			// Multiply sound distance volume play decision by
			// EnviroSound.soundDistanceAllowance
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETSTATIC, "net/alkalus/envirosound/EnviroSound", "soundDistanceAllowance", "D"
					)
			);
			toInject.add(new InsnNode(Opcodes.DMUL));

			// Target method: sendToAllNearExcept
			bytes = patchMethodInClass(
					obfuscated, bytes, "a", "(Lyz;DDDDILft;)V", Opcodes.DCMPG, AbstractInsnNode.INSN, "", "", -1, toInject, true, 0, 0, false, 0
			);
		}
		else

		/*if (obfuscated.equals("vg")) {
		// Inside Entity
		InsnList toInject = new InsnList();
		
		// Offset entity sound by their eye height
		toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));
		toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/alkalus/envirosound/EnviroSound",
		"calculateEntitySoundOffset", "(Lvg;Lqe;)D", false));
		toInject.add(new InsnNode(Opcodes.DADD));
		toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
		
		// Target method: playSound
		// Inside target method, target node: Entity/getSoundCategory
		bytes = patchMethodInClass(obfuscated, bytes, "a", "(Lqe;FF)V", Opcodes.INVOKEVIRTUAL,
		AbstractInsnNode.METHOD_INSN, "bK", null, toInject, true, 0, 0, false, -3);
		} else*/
		if (obfuscated.equals("ahb")) {
			// Inside World
			final InsnList toInject = new InsnList();

			// Offset entity sound by their eye height
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 2));
			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKESTATIC, "net/alkalus/envirosound/EnviroSound", "calculateEntitySoundOffset", "(Lsa;Ljava/lang/String;)D", false
					)
			);
			toInject.add(new InsnNode(Opcodes.DADD));
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));

			// Target method: playSoundAtEntity
			bytes = patchMethodInClass(
					obfuscated, bytes, "a", "(Lsa;Ljava/lang/String;FF)V", Opcodes.INVOKEINTERFACE, AbstractInsnNode.METHOD_INSN, "a", null, -1, toInject, true, 0, 0, false, -3
			);

			/*// Offset entity sound by their eye height
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 2));
			toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/alkalus/envirosound/EnviroSound",
			"calculateEntitySoundOffsetPlayer", "(Lyz;Ljava/lang/String;)D", false));
			toInject.add(new InsnNode(Opcodes.DADD));
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));
			
			// Target method: playSoundToNearExcept
			bytes = patchMethodInClass(obfuscated, bytes, "a", "(Lyz;Ljava/lang/String;FF)V", Opcodes.INVOKEINTERFACE,
			AbstractInsnNode.METHOD_INSN, "a", null, -1, toInject, true, 0, 0, false, -3);*/
		}
		else

		// Fix for computronics's sound card and tape drive
		if (obfuscated.equals("pl.asie.lib.audio.StreamingAudioPlayer")
				&& Config.computronicsPatching) {
			// Inside StreamingAudioPlayer
			InsnList toInject = new InsnList();

			toInject.add(new VarInsnNode(Opcodes.FLOAD, 2));
			toInject.add(new VarInsnNode(Opcodes.FLOAD, 3));
			toInject.add(new VarInsnNode(Opcodes.FLOAD, 4));
			toInject.add(new VarInsnNode(Opcodes.ALOAD, 8));
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "pl/asie/lib/audio/StreamingAudioPlayer$SourceEntry", "src", "Ljava/nio/IntBuffer;"
					)
			);
			toInject.add(new InsnNode(Opcodes.ICONST_0));
			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKEVIRTUAL, "java/nio/IntBuffer", "get", "(I)I", false
					)
			);

			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKESTATIC, "net/alkalus/envirosound/EnviroSound", "onPlaySoundAL", "(FFFI)V", false
					)
			);

			// Target method: play
			bytes = patchMethodInClass(
					obfuscated, bytes, "play", "(Ljava/lang/String;FFFF)V", Opcodes.INVOKESTATIC, AbstractInsnNode.METHOD_INSN, "alSourceQueueBuffers", null, -1, toInject, true, 0, 0, false, -5
			);

			toInject = new InsnList();

			toInject.add(
					new FieldInsnNode(
							Opcodes.GETSTATIC, "net/alkalus/envirosound/EnviroSound", "soundDistanceAllowance", "D"
					)
			);
			toInject.add(new InsnNode(Opcodes.D2F));
			toInject.add(new InsnNode(Opcodes.FMUL));

			// Target method: setHearing
			bytes = patchMethodInClass(
					obfuscated, bytes, "setHearing", "(FF)V", Opcodes.FLOAD, AbstractInsnNode.VAR_INSN, "", null, 1, toInject, false, 0, 0, false, 0
			);
		}
		else

		if (obfuscated.equals("pl.asie.computronics.api.audio.AudioPacket")
				&& Config.computronicsPatching) {
			// Inside AudioPacket
			final InsnList toInject = new InsnList();

			toInject.add(
					new FieldInsnNode(
							Opcodes.GETSTATIC, "net/alkalus/envirosound/EnviroSound", "soundDistanceAllowance", "D"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETSTATIC, "net/alkalus/envirosound/EnviroSound", "soundDistanceAllowance", "D"
					)
			);
			toInject.add(new InsnNode(Opcodes.DMUL));
			toInject.add(new InsnNode(Opcodes.D2I));
			toInject.add(new InsnNode(Opcodes.IMUL));

			// Target method: canHearReceiver
			bytes = patchMethodInClass(
					obfuscated, bytes, "canHearReceiver", "(Lnet/minecraft/entity/player/EntityPlayerMP;Lpl/asie/computronics/api/audio/IAudioReceiver;)Z", Opcodes.IMUL, AbstractInsnNode.INSN, "", null, -1, toInject, false, 0, 0, false, 0
			);
		}
		else

		if (obfuscated.equals("pl.asie.computronics.tile.TileTapeDrive$1")
				&& Config.computronicsPatching) {
			// Inside TileTapeDrive.internalSpeaker
			InsnList toInject = new InsnList();

			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "pl/asie/computronics/tile/TileTapeDrive$1", "this$0", "Lpl/asie/computronics/tile/TileTapeDrive;"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETSTATIC, "pl/asie/computronics/Computronics", "tapeReader", "Lpl/asie/computronics/block/BlockTapeReader;"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "pl/asie/computronics/block/BlockTapeReader", "rotation", "Lpl/asie/lib/block/BlockBase$Rotation;"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "pl/asie/lib/block/BlockBase$Rotation", "FACING", "Lnet/minecraft/block/properties/PropertyDirection;"
					)
			);
			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKESTATIC, "com/sonicether/soundphysics/SoundPhysics", "computronicsOffset", "(Lnet/minecraft/util/Vec3;Lnet/minecraft/tileentity/TileEntity;)Lnet/minecraft/util/Vec3;", false
					)
			);

			// Target method: getSoundPos
			//bytes = patchMethodInClass(obfuscated, bytes, "getSoundPos", "()Lnet/minecraft/util/math/Vec3d;", Opcodes.ARETURN, AbstractInsnNode.INSN, "", null, -1, toInject, true, 0, 0, false, 0);
		}
		else

		if (obfuscated.equals("pl.asie.computronics.tile.TileSpeaker")
				&& Config.computronicsPatching) {
			// Inside TileSpeaker
			InsnList toInject = new InsnList();

			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETSTATIC, "pl/asie/computronics/Computronics", "speaker", "Lpl/asie/computronics/block/BlockSpeaker;"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "pl/asie/computronics/block/BlockSpeaker", "rotation", "Lpl/asie/lib/block/BlockBase$Rotation;"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "pl/asie/lib/block/BlockBase$Rotation", "FACING", "Lnet/minecraft/block/properties/PropertyDirection;"
					)
			);
			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKESTATIC, "com/sonicether/soundphysics/SoundPhysics", "computronicsOffset", "(Lnet/minecraft/util/Vec3;Lnet/minecraft/tileentity/TileEntity;)Lnet/minecraft/util/Vec3;", false
					)
			);

			// Target method: getSoundPos
			//bytes = patchMethodInClass(obfuscated, bytes, "getSoundPos", "()Lnet/minecraft/util/math/Vec3d;", Opcodes.ARETURN, AbstractInsnNode.INSN, "", null, -1, toInject, true, 0, 0, false, 0);
		}
		else

		if (obfuscated.equals("pl.asie.computronics.tile.TileSpeechBox$1")
				&& Config.computronicsPatching) {
			// Inside TileSpeechBox.internalSpeaker
			InsnList toInject = new InsnList();

			toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "pl/asie/computronics/tile/TileSpeechBox$1", "this$0", "Lpl/asie/computronics/tile/TileSpeechBox;"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETSTATIC, "pl/asie/computronics/Computronics", "speechBox", "Lpl/asie/computronics/block/BlockSpeechBox;"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "pl/asie/computronics/block/BlockSpeechBox", "rotation", "Lpl/asie/lib/block/BlockBase$Rotation;"
					)
			);
			toInject.add(
					new FieldInsnNode(
							Opcodes.GETFIELD, "pl/asie/lib/block/BlockBase$Rotation", "FACING", "Lnet/minecraft/block/properties/PropertyDirection;"
					)
			);
			toInject.add(
					new MethodInsnNode(
							Opcodes.INVOKESTATIC, "com/sonicether/soundphysics/SoundPhysics", "computronicsOffset", "(Lnet/minecraft/util/Vec3;Lnet/minecraft/tileentity/TileEntity;)Lnet/minecraft/util/Vec3;", false
					)
			);

			// Target method: getSoundPos
			//bytes = patchMethodInClass(obfuscated, bytes, "getSoundPos", "()Lnet/minecraft/util/math/Vec3d;", Opcodes.ARETURN, AbstractInsnNode.INSN, "", null, -1, toInject, true, 0, 0, false, 0);
		}

		// System.out.println("[SP Inject] "+obfuscated+" ("+deobfuscated+")");

		return bytes;
	}
}
