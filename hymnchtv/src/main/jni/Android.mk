LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CFLAGS    := -DOPENCC_ENABLE_DARTS
LOCAL_LDFLAGS += "-Wl,-z,max-page-size=16384"

LOCAL_MODULE    := OpenCC
LOCAL_C_INCLUDES += src/main/jni/OpenCC/deps/darts-clone-0.32/
LOCAL_C_INCLUDES += src/main/jni/OpenCC/deps/marisa-0.2.6/include/
LOCAL_C_INCLUDES += src/main/jni/OpenCC/deps/marisa-0.2.6/lib/
LOCAL_C_INCLUDES += src/main/jni/OpenCC/deps/rapidjson-1.1.0/
LOCAL_C_INCLUDES += src/main/jni/OpenCC/deps/tclap-1.2.5/tclap

LOCAL_SRC_FILES := \
OpenCC/src/BinaryDict.cpp \
OpenCC/src/Config.cpp \
OpenCC/src/Conversion.cpp\
OpenCC/src/ConversionChain.cpp \
OpenCC/src/Converter.cpp \
OpenCC/src/DartsDict.cpp \
OpenCC/src/Dict.cpp \
OpenCC/src/DictConverter.cpp \
OpenCC/src/DictEntry.cpp \
OpenCC/src/DictGroup.cpp \
OpenCC/src/Lexicon.cpp \
OpenCC/src/MarisaDict.cpp \
OpenCC/src/MaxMatchSegmentation.cpp \
OpenCC/src/PhraseExtract.cpp \
OpenCC/src/Segmentation.cpp \
OpenCC/src/SerializedValues.cpp \
OpenCC/src/SimpleConverter.cpp \
OpenCC/src/TextDict.cpp \
OpenCC/src/UTF8StringSlice.cpp \
OpenCC/src/UTF8Util.cpp \
OpenCC/deps/marisa-0.2.6/lib/marisa/agent.cc \
OpenCC/deps/marisa-0.2.6/lib/marisa/keyset.cc \
OpenCC/deps/marisa-0.2.6/lib/marisa/trie.cc \
OpenCC/deps/marisa-0.2.6/lib/marisa/grimoire/io/mapper.cc \
OpenCC/deps/marisa-0.2.6/lib/marisa/grimoire/io/reader.cc \
OpenCC/deps/marisa-0.2.6/lib/marisa/grimoire/io/writer.cc \
OpenCC/deps/marisa-0.2.6/lib/marisa/grimoire/trie/louds-trie.cc \
OpenCC/deps/marisa-0.2.6/lib/marisa/grimoire/trie/tail.cc \
OpenCC/deps/marisa-0.2.6/lib/marisa/grimoire/vector/bit-vector.cc

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_PRELINK_MODULE := false

LOCAL_MODULE 	:= ChineseConverter
LOCAL_C_INCLUDES += src/main/jni/OpenCC/src/
LOCAL_STATIC_LIBRARIES := OpenCC
LOCAL_LDLIBS  += -llog -landroid

LOCAL_SRC_FILES := chineseconverter.cpp

include $(BUILD_SHARED_LIBRARY)