package wraplibvorbis;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : ogg/ogg.h:65</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class ogg_sync_state extends Structure {
	/** C type : unsigned char* */
	public Pointer data;
	public int storage;
	public int fill;
	public int returned;
	public int unsynced;
	public int headerbytes;
	public int bodybytes;
	public ogg_sync_state() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("data", "storage", "fill", "returned", "unsynced", "headerbytes", "bodybytes");
	}
	/** @param data C type : unsigned char* */
	public ogg_sync_state(Pointer data, int storage, int fill, int returned, int unsynced, int headerbytes, int bodybytes) {
		super();
		this.data = data;
		this.storage = storage;
		this.fill = fill;
		this.returned = returned;
		this.unsynced = unsynced;
		this.headerbytes = headerbytes;
		this.bodybytes = bodybytes;
	}
	public static class ByReference extends ogg_sync_state implements Structure.ByReference {
		
	};
	public static class ByValue extends ogg_sync_state implements Structure.ByValue {
		
	};
}
