
#include <jni.h>
#include <sys/types.h>
#include <android/log.h>

#include <cassert>
#include <fstream>
#include <iostream>

#include <json-cpp/json.h>
//#include <JSONCPP/json/json.h>

#include <vtkIterativeClosestPointTransform.h>
#include <vtkSmartPointer.h>

#define  LOG_TAG    "MADN3SController"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

namespace {
//----------------------------------------------------------------------------
    bool testLog(const std::string& message){
        LOGI("Native Testing Log: %s", message.c_str());
        Json::Value root;
        Json::Reader reader;
        bool parsingSuccessful = reader.parse( message, root);
        if ( !parsingSuccessful )
        {
            std::cout  << "Failed to parse configuration\n"
                       << reader.getFormattedErrorMessages();
            return false;
        }

        std::string result = root.get("hello", "midget" ).asString();
        LOGI("Native JSONParsing Result: %s",  result.c_str());

        LOGI("Native testLog. instantiating ICP.");
        vtkSmartPointer<vtkIterativeClosestPointTransform> icp =
              vtkSmartPointer<vtkIterativeClosestPointTransform>::New();
        LOGI("Native testLog. ICP instance success.");

        return true;
    }
};
//----------------------------------------------------------------------------
extern "C" {
  JNIEXPORT jboolean JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_testLog(JNIEnv * env, jobject obj, jstring message);
};

JNIEXPORT jboolean JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_testLog(JNIEnv * env, jobject obj,  jstring message)
{
	const char *javaStr = env->GetStringUTFChars(message, NULL);
	LOGI("JNIExport Testing Log: %s", javaStr);

	  if (javaStr) {
		std::string messageStr = javaStr;
		return testLog(messageStr);
	  }
	return false;
}

