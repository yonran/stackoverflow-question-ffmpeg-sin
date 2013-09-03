package wraplibvorbis;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import wraplibvorbis.ov_callbacks.ByValue;
/**
 * JNA Wrapper for library <b>vorbisfile</b><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public interface VorbisfileLibrary extends Library {
	public static final String JNA_LIBRARY_NAME = "vorbisfile";
//	public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(VorbisfileLibrary.JNA_LIBRARY_NAME);
//	public static final VorbisfileLibrary INSTANCE = (VorbisfileLibrary)Native.loadLibrary(VorbisfileLibrary.JNA_LIBRARY_NAME, VorbisfileLibrary.class);
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h</i> */
	public static final int STREAMSET = (int)3;
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h</i> */
	public static final int NOTOPEN = (int)0;
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h</i> */
	public static final int PARTOPEN = (int)1;
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h</i> */
	public static final int INITSET = (int)4;
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h</i> */
	public static final int OPENED = (int)2;
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:134</i> */
	public interface OV_CALLBACKS_DEFAULT_expression_callback extends Callback {
		NativeSize apply(Pointer voidPtr1, NativeSize size_t1, NativeSize size_t2, Pointer voidPtr2);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:135</i> */
	public interface OV_CALLBACKS_DEFAULT_expression_callback2 extends Callback {
		int apply(Pointer voidPtr1, int int1);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:136</i> */
	public interface OV_CALLBACKS_DEFAULT_expression_callback3 extends Callback {
		int apply(Pointer voidPtr1);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:137</i> */
	public interface OV_CALLBACKS_DEFAULT_expression_callback4 extends Callback {
		NativeLong apply(Pointer voidPtr1);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:138</i> */
	public interface OV_CALLBACKS_NOCLOSE_expression_callback extends Callback {
		NativeSize apply(Pointer voidPtr1, NativeSize size_t1, NativeSize size_t2, Pointer voidPtr2);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:139</i> */
	public interface OV_CALLBACKS_NOCLOSE_expression_callback2 extends Callback {
		int apply(Pointer voidPtr1, int int1);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:140</i> */
	public interface OV_CALLBACKS_NOCLOSE_expression_callback3 extends Callback {
		int apply(Pointer voidPtr1);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:141</i> */
	public interface OV_CALLBACKS_NOCLOSE_expression_callback4 extends Callback {
		NativeLong apply(Pointer voidPtr1);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:142</i> */
	public interface OV_CALLBACKS_STREAMONLY_expression_callback extends Callback {
		NativeSize apply(Pointer voidPtr1, NativeSize size_t1, NativeSize size_t2, Pointer voidPtr2);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:143</i> */
	public interface OV_CALLBACKS_STREAMONLY_expression_callback2 extends Callback {
		int apply(Pointer voidPtr1, int int1);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:144</i> */
	public interface OV_CALLBACKS_STREAMONLY_expression_callback3 extends Callback {
		int apply(Pointer voidPtr1);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:145</i> */
	public interface OV_CALLBACKS_STREAMONLY_expression_callback4 extends Callback {
		NativeLong apply(Pointer voidPtr1);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:146</i> */
	public interface OV_CALLBACKS_STREAMONLY_NOCLOSE_expression_callback extends Callback {
		NativeSize apply(Pointer voidPtr1, NativeSize size_t1, NativeSize size_t2, Pointer voidPtr2);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:147</i> */
	public interface OV_CALLBACKS_STREAMONLY_NOCLOSE_expression_callback2 extends Callback {
		int apply(Pointer voidPtr1, int int1);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:148</i> */
	public interface OV_CALLBACKS_STREAMONLY_NOCLOSE_expression_callback3 extends Callback {
		int apply(Pointer voidPtr1);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:149</i> */
	public interface OV_CALLBACKS_STREAMONLY_NOCLOSE_expression_callback4 extends Callback {
		NativeLong apply(Pointer voidPtr1);
	};
	/** <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:150</i> */
	public interface ov_read_filter_filter_callback extends Callback {
		void apply(PointerByReference pcm, NativeLong channels, NativeLong samples, Pointer filter_param);
	};
	/**
	 * Original signature : <code>int _ov_header_fseek_wrap(FILE*, ogg_int64_t, int)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:25</i><br>
	 * @deprecated use the safer method {@link #_ov_header_fseek_wrap(com.sun.jna.ptr.PointerByReference, long, int)} instead
	 */
	@Deprecated 
	int _ov_header_fseek_wrap(Pointer f, long off, int whence);
	/**
	 * Original signature : <code>int _ov_header_fseek_wrap(FILE*, ogg_int64_t, int)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:25</i>
	 */
	int _ov_header_fseek_wrap(PointerByReference f, long off, int whence);
	/**
	 * Original signature : <code>int ov_clear(OggVorbis_File*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:63</i>
	 */
	int ov_clear(OggVorbis_File vf);
	/**
	 * Original signature : <code>int ov_fopen(const char*, OggVorbis_File*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:65</i><br>
	 * @deprecated use the safer methods {@link #ov_fopen(java.lang.String, wraplibvorbis.OggVorbis_File)} and {@link #ov_fopen(com.sun.jna.Pointer, wraplibvorbis.OggVorbis_File)} instead
	 */
	@Deprecated 
	int ov_fopen(Pointer path, OggVorbis_File vf);
	/**
	 * Original signature : <code>int ov_fopen(const char*, OggVorbis_File*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:65</i>
	 */
	int ov_fopen(String path, OggVorbis_File vf);
	/**
	 * Original signature : <code>int ov_open(FILE*, OggVorbis_File*, const char*, long)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:67</i><br>
	 * @deprecated use the safer methods {@link #ov_open(com.sun.jna.ptr.PointerByReference, wraplibvorbis.OggVorbis_File, java.lang.String, com.sun.jna.NativeLong)} and {@link #ov_open(com.sun.jna.ptr.PointerByReference, wraplibvorbis.OggVorbis_File, com.sun.jna.Pointer, com.sun.jna.NativeLong)} instead
	 */
	@Deprecated 
	int ov_open(Pointer f, OggVorbis_File vf, Pointer initial, NativeLong ibytes);
	/**
	 * Original signature : <code>int ov_open(FILE*, OggVorbis_File*, const char*, long)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:67</i>
	 */
	int ov_open(PointerByReference f, OggVorbis_File vf, String initial, NativeLong ibytes);
	/**
	 * Original signature : <code>int ov_open(FILE*, OggVorbis_File*, const char*, long)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:67</i>
	 */
	int ov_open(PointerByReference f, OggVorbis_File vf, Pointer initial, NativeLong ibytes);
	/**
	 * Original signature : <code>int ov_open_callbacks(void*, OggVorbis_File*, const char*, long, ov_callbacks)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:69</i><br>
	 * @deprecated use the safer methods {@link #ov_open_callbacks(com.sun.jna.Pointer, wraplibvorbis.OggVorbis_File, java.lang.String, com.sun.jna.NativeLong, wraplibvorbis.ov_callbacks.ByValue)} and {@link #ov_open_callbacks(com.sun.jna.Pointer, wraplibvorbis.OggVorbis_File, com.sun.jna.Pointer, com.sun.jna.NativeLong, wraplibvorbis.ov_callbacks.ByValue)} instead
	 */
	@Deprecated 
	int ov_open_callbacks(Pointer datasource, OggVorbis_File vf, Pointer initial, NativeLong ibytes, ByValue callbacks);
	/**
	 * Original signature : <code>int ov_open_callbacks(void*, OggVorbis_File*, const char*, long, ov_callbacks)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:69</i>
	 */
	int ov_open_callbacks(Pointer datasource, OggVorbis_File vf, String initial, NativeLong ibytes, ByValue callbacks);
	/**
	 * Original signature : <code>int ov_test(FILE*, OggVorbis_File*, const char*, long)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:71</i><br>
	 * @deprecated use the safer methods {@link #ov_test(com.sun.jna.ptr.PointerByReference, wraplibvorbis.OggVorbis_File, java.lang.String, com.sun.jna.NativeLong)} and {@link #ov_test(com.sun.jna.ptr.PointerByReference, wraplibvorbis.OggVorbis_File, com.sun.jna.Pointer, com.sun.jna.NativeLong)} instead
	 */
	@Deprecated 
	int ov_test(Pointer f, OggVorbis_File vf, Pointer initial, NativeLong ibytes);
	/**
	 * Original signature : <code>int ov_test(FILE*, OggVorbis_File*, const char*, long)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:71</i>
	 */
	int ov_test(PointerByReference f, OggVorbis_File vf, String initial, NativeLong ibytes);
	/**
	 * Original signature : <code>int ov_test(FILE*, OggVorbis_File*, const char*, long)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:71</i>
	 */
	int ov_test(PointerByReference f, OggVorbis_File vf, Pointer initial, NativeLong ibytes);
	/**
	 * Original signature : <code>int ov_test_callbacks(void*, OggVorbis_File*, const char*, long, ov_callbacks)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:73</i><br>
	 * @deprecated use the safer methods {@link #ov_test_callbacks(com.sun.jna.Pointer, wraplibvorbis.OggVorbis_File, java.lang.String, com.sun.jna.NativeLong, wraplibvorbis.ov_callbacks.ByValue)} and {@link #ov_test_callbacks(com.sun.jna.Pointer, wraplibvorbis.OggVorbis_File, com.sun.jna.Pointer, com.sun.jna.NativeLong, wraplibvorbis.ov_callbacks.ByValue)} instead
	 */
	@Deprecated 
	int ov_test_callbacks(Pointer datasource, OggVorbis_File vf, Pointer initial, NativeLong ibytes, ByValue callbacks);
	/**
	 * Original signature : <code>int ov_test_callbacks(void*, OggVorbis_File*, const char*, long, ov_callbacks)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:73</i>
	 */
	int ov_test_callbacks(Pointer datasource, OggVorbis_File vf, String initial, NativeLong ibytes, ByValue callbacks);
	/**
	 * Original signature : <code>int ov_test_open(OggVorbis_File*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:75</i>
	 */
	int ov_test_open(OggVorbis_File vf);
	/**
	 * Original signature : <code>long ov_bitrate(OggVorbis_File*, int)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:77</i>
	 */
	NativeLong ov_bitrate(OggVorbis_File vf, int i);
	/**
	 * Original signature : <code>long ov_bitrate_instant(OggVorbis_File*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:79</i>
	 */
	NativeLong ov_bitrate_instant(OggVorbis_File vf);
	/**
	 * Original signature : <code>long ov_streams(OggVorbis_File*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:81</i>
	 */
	NativeLong ov_streams(OggVorbis_File vf);
	/**
	 * Original signature : <code>long ov_seekable(OggVorbis_File*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:83</i>
	 */
	NativeLong ov_seekable(OggVorbis_File vf);
	/**
	 * Original signature : <code>long ov_serialnumber(OggVorbis_File*, int)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:85</i>
	 */
	NativeLong ov_serialnumber(OggVorbis_File vf, int i);
	/**
	 * Original signature : <code>ogg_int64_t ov_raw_total(OggVorbis_File*, int)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:87</i>
	 */
	long ov_raw_total(OggVorbis_File vf, int i);
	/**
	 * Original signature : <code>ogg_int64_t ov_pcm_total(OggVorbis_File*, int)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:89</i>
	 */
	long ov_pcm_total(OggVorbis_File vf, int i);
	/**
	 * Original signature : <code>double ov_time_total(OggVorbis_File*, int)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:91</i>
	 */
	double ov_time_total(OggVorbis_File vf, int i);
	/**
	 * Original signature : <code>int ov_raw_seek(OggVorbis_File*, ogg_int64_t)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:93</i>
	 */
	int ov_raw_seek(OggVorbis_File vf, long pos);
	/**
	 * Original signature : <code>int ov_pcm_seek(OggVorbis_File*, ogg_int64_t)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:95</i>
	 */
	int ov_pcm_seek(OggVorbis_File vf, long pos);
	/**
	 * Original signature : <code>int ov_pcm_seek_page(OggVorbis_File*, ogg_int64_t)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:97</i>
	 */
	int ov_pcm_seek_page(OggVorbis_File vf, long pos);
	/**
	 * Original signature : <code>int ov_time_seek(OggVorbis_File*, double)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:99</i>
	 */
	int ov_time_seek(OggVorbis_File vf, double pos);
	/**
	 * Original signature : <code>int ov_time_seek_page(OggVorbis_File*, double)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:101</i>
	 */
	int ov_time_seek_page(OggVorbis_File vf, double pos);
	/**
	 * Original signature : <code>int ov_raw_seek_lap(OggVorbis_File*, ogg_int64_t)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:103</i>
	 */
	int ov_raw_seek_lap(OggVorbis_File vf, long pos);
	/**
	 * Original signature : <code>int ov_pcm_seek_lap(OggVorbis_File*, ogg_int64_t)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:105</i>
	 */
	int ov_pcm_seek_lap(OggVorbis_File vf, long pos);
	/**
	 * Original signature : <code>int ov_pcm_seek_page_lap(OggVorbis_File*, ogg_int64_t)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:107</i>
	 */
	int ov_pcm_seek_page_lap(OggVorbis_File vf, long pos);
	/**
	 * Original signature : <code>int ov_time_seek_lap(OggVorbis_File*, double)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:109</i>
	 */
	int ov_time_seek_lap(OggVorbis_File vf, double pos);
	/**
	 * Original signature : <code>int ov_time_seek_page_lap(OggVorbis_File*, double)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:111</i>
	 */
	int ov_time_seek_page_lap(OggVorbis_File vf, double pos);
	/**
	 * Original signature : <code>ogg_int64_t ov_raw_tell(OggVorbis_File*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:113</i>
	 */
	long ov_raw_tell(OggVorbis_File vf);
	/**
	 * Original signature : <code>ogg_int64_t ov_pcm_tell(OggVorbis_File*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:115</i>
	 */
	long ov_pcm_tell(OggVorbis_File vf);
	/**
	 * Original signature : <code>double ov_time_tell(OggVorbis_File*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:117</i>
	 */
	double ov_time_tell(OggVorbis_File vf);
	/**
	 * Original signature : <code>vorbis_info* ov_info(OggVorbis_File*, int)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:119</i>
	 */
	vorbis_info ov_info(OggVorbis_File vf, int link);
	/**
	 * Original signature : <code>vorbis_comment* ov_comment(OggVorbis_File*, int)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:121</i>
	 */
	vorbis_comment ov_comment(OggVorbis_File vf, int link);
	/**
	 * Original signature : <code>long ov_read_float(OggVorbis_File*, float***, int, int*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:123</i><br>
	 * @deprecated use the safer methods {@link #ov_read_float(wraplibvorbis.OggVorbis_File, com.sun.jna.ptr.PointerByReference, int, java.nio.IntBuffer)} and {@link #ov_read_float(wraplibvorbis.OggVorbis_File, com.sun.jna.ptr.PointerByReference, int, com.sun.jna.ptr.IntByReference)} instead
	 */
	@Deprecated 
	NativeLong ov_read_float(OggVorbis_File vf, PointerByReference pcm_channels, int samples, IntByReference bitstream);
	/**
	 * Original signature : <code>long ov_read_float(OggVorbis_File*, float***, int, int*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:123</i>
	 */
	NativeLong ov_read_float(OggVorbis_File vf, PointerByReference pcm_channels, int samples, IntBuffer bitstream);
	/**
	 * Original signature : <code>long ov_read_filter(OggVorbis_File*, char*, int, int, int, int, int*, ov_read_filter_filter_callback*, void*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:125</i><br>
	 * @deprecated use the safer methods {@link #ov_read_filter(wraplibvorbis.OggVorbis_File, java.nio.ByteBuffer, int, int, int, int, java.nio.IntBuffer, wraplibvorbis.VorbisfileLibrary.ov_read_filter_filter_callback, com.sun.jna.Pointer)} and {@link #ov_read_filter(wraplibvorbis.OggVorbis_File, com.sun.jna.Pointer, int, int, int, int, com.sun.jna.ptr.IntByReference, wraplibvorbis.VorbisfileLibrary.ov_read_filter_filter_callback, com.sun.jna.Pointer)} instead
	 */
	@Deprecated 
	NativeLong ov_read_filter(OggVorbis_File vf, Pointer buffer, int length, int bigendianp, int word, int sgned, IntByReference bitstream, VorbisfileLibrary.ov_read_filter_filter_callback filter, Pointer filter_param);
	/**
	 * Original signature : <code>long ov_read_filter(OggVorbis_File*, char*, int, int, int, int, int*, ov_read_filter_filter_callback*, void*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:125</i>
	 */
	NativeLong ov_read_filter(OggVorbis_File vf, ByteBuffer buffer, int length, int bigendianp, int word, int sgned, IntBuffer bitstream, VorbisfileLibrary.ov_read_filter_filter_callback filter, Pointer filter_param);
	/**
	 * Original signature : <code>long ov_read(OggVorbis_File*, char*, int, int, int, int, int*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:127</i><br>
	 * @deprecated use the safer methods {@link #ov_read(wraplibvorbis.OggVorbis_File, java.nio.ByteBuffer, int, int, int, int, java.nio.IntBuffer)} and {@link #ov_read(wraplibvorbis.OggVorbis_File, com.sun.jna.Pointer, int, int, int, int, com.sun.jna.ptr.IntByReference)} instead
	 */
	@Deprecated 
	NativeLong ov_read(OggVorbis_File vf, Pointer buffer, int length, int bigendianp, int word, int sgned, IntByReference bitstream);
	/**
	 * Original signature : <code>long ov_read(OggVorbis_File*, char*, int, int, int, int, int*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:127</i>
	 */
	NativeLong ov_read(OggVorbis_File vf, ByteBuffer buffer, int length, int bigendianp, int word, int sgned, IntBuffer bitstream);
	/**
	 * Original signature : <code>int ov_crosslap(OggVorbis_File*, OggVorbis_File*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:129</i>
	 */
	int ov_crosslap(OggVorbis_File vf1, OggVorbis_File vf2);
	/**
	 * Original signature : <code>int ov_halfrate(OggVorbis_File*, int)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:131</i>
	 */
	int ov_halfrate(OggVorbis_File vf, int flag);
	/**
	 * Original signature : <code>int ov_halfrate_p(OggVorbis_File*)</code><br>
	 * <i>native declaration : /home/yonran/Downloads/libvorbis-1.3.3/include/vorbis/vorbisfile.h:133</i>
	 */
	int ov_halfrate_p(OggVorbis_File vf);
	public static class FILE extends PointerType {
		public FILE(Pointer address) {
			super(address);
		}
		public FILE() {
			super();
		}
	};
}
