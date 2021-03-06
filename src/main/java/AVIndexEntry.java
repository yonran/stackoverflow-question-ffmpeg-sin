
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : /usr/include/libavformat/avformat.h:7807</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public abstract class AVIndexEntry extends Structure {
	public long pos;
	public long timestamp;
	/** Conversion Error : flags:2 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
	/** Conversion Error : size:30 (This runtime does not support bit fields : JNA (please use BridJ instead)) */
	/** < Minimum distance between this and the previous keyframe, used to avoid unneeded searching. */
	public int min_distance;
	public AVIndexEntry() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("pos", "timestamp", "min_distance");
	}
	/** @param min_distance < Minimum distance between this and the previous keyframe, used to avoid unneeded searching. */
	public AVIndexEntry(long pos, long timestamp, int min_distance) {
		super();
		this.pos = pos;
		this.timestamp = timestamp;
		this.min_distance = min_distance;
	}
	public static abstract class ByReference extends AVIndexEntry implements Structure.ByReference {
		
	};
	public static abstract class ByValue extends AVIndexEntry implements Structure.ByValue {
		
	};
}
