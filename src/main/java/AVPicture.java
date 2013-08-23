
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : /usr/include/libavcodec/avcodec.h:5838</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class AVPicture extends Structure {
	/** C type : uint8_t*[4] */
	public Pointer[] data = new Pointer[4];
	/**
	 * < number of bytes per line<br>
	 * C type : int[4]
	 */
	public int[] linesize = new int[4];
	public AVPicture() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("data", "linesize");
	}
	/**
	 * @param data C type : uint8_t*[4]<br>
	 * @param linesize < number of bytes per line<br>
	 * C type : int[4]
	 */
	public AVPicture(Pointer data[], int linesize[]) {
		super();
		if ((data.length != this.data.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.data = data;
		if ((linesize.length != this.linesize.length)) 
			throw new IllegalArgumentException("Wrong array size !");
		this.linesize = linesize;
	}
	public static class ByReference extends AVPicture implements Structure.ByReference {
		
	};
	public static class ByValue extends AVPicture implements Structure.ByValue {
		
	};
}
