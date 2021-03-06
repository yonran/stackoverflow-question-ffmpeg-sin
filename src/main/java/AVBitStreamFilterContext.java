
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
/**
 * <i>native declaration : /usr/include/libavcodec/avcodec.h:6800</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class AVBitStreamFilterContext extends Structure {
	/** C type : void* */
	public Pointer priv_data;
	/** C type : AVBitStreamFilter* */
	public AVBitStreamFilter.ByReference filter;
	/** C type : AVCodecParserContext* */
	public AVCodecParserContext.ByReference parser;
	/** C type : AVBitStreamFilterContext* */
	public AVBitStreamFilterContext.ByReference next;
	public AVBitStreamFilterContext() {
		super();
	}
	protected List<? > getFieldOrder() {
		return Arrays.asList("priv_data", "filter", "parser", "next");
	}
	/**
	 * @param priv_data C type : void*<br>
	 * @param filter C type : AVBitStreamFilter*<br>
	 * @param parser C type : AVCodecParserContext*<br>
	 * @param next C type : AVBitStreamFilterContext*
	 */
	public AVBitStreamFilterContext(Pointer priv_data, AVBitStreamFilter.ByReference filter, AVCodecParserContext.ByReference parser, AVBitStreamFilterContext.ByReference next) {
		super();
		this.priv_data = priv_data;
		this.filter = filter;
		this.parser = parser;
		this.next = next;
	}
	public static class ByReference extends AVBitStreamFilterContext implements Structure.ByReference {
		
	};
	public static class ByValue extends AVBitStreamFilterContext implements Structure.ByValue {
		
	};
}
