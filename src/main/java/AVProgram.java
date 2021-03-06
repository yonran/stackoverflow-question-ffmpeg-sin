
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : /usr/include/libavformat/avformat.h:7939</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class AVProgram extends Structure {
	public int id;
	public int flags;
	/**
	 * @see com.acuitus.wrapffmpeg.AvcodecLibrary#AVDiscard<br>
	 * < selects which program to discard and which to feed to the caller<br>
	 * C type : AVDiscard
	 */
	public int discard;
	/** C type : unsigned int* */
	public IntByReference stream_index;
	public int nb_stream_indexes;
	/** C type : AVDictionary* */
	public PointerByReference metadata;
	public AVProgram() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("id", "flags", "discard", "stream_index", "nb_stream_indexes", "metadata");
	}
	/**
	 * @param discard @see com.acuitus.wrapffmpeg.AvcodecLibrary#AVDiscard<br>
	 * < selects which program to discard and which to feed to the caller<br>
	 * C type : AVDiscard<br>
	 * @param stream_index C type : unsigned int*<br>
	 * @param metadata C type : AVDictionary*
	 */
	public AVProgram(int id, int flags, int discard, IntByReference stream_index, int nb_stream_indexes, PointerByReference metadata) {
		super();
		this.id = id;
		this.flags = flags;
		this.discard = discard;
		this.stream_index = stream_index;
		this.nb_stream_indexes = nb_stream_indexes;
		this.metadata = metadata;
	}
	public static class ByReference extends AVProgram implements Structure.ByReference {
		
	};
	public static class ByValue extends AVProgram implements Structure.ByValue {
		
	};
}
