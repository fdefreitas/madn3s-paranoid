
#include <jni.h>
#include <sys/types.h>
#include <android/log.h>

#include <cassert>
#include <fstream>
#include <iostream>
#include <string>

#include <json-cpp/json.h>
//#include <JSONCPP/json/json.h>

#include <vtkIterativeClosestPointTransform.h>
#include <vtkSmartPointer.h>
#include <vtkXMLPolyDataReader.h>

#include <vtkVersion.h>
#include <vtkTransform.h>
#include <vtkVertexGlyphFilter.h>
#include <vtkPoints.h>
#include <vtkPolyData.h>
#include <vtkCleanPolyData.h>
#include <vtkDelaunay3D.h>
#include <vtkCellArray.h>
#include <vtkTransformPolyDataFilter.h>
#include <vtkLandmarkTransform.h>
#include <vtkMatrix4x4.h>
#include <vtkXMLPolyDataWriter.h>
#include <vtkPolyDataMapper.h>
#include <vtkActor.h>
#include <vtkRenderWindow.h>
#include <vtkRenderer.h>
#include <vtkRenderWindowInteractor.h>
#include <vtkProperty.h>

#define  LOG_TAG    "MADN3SController_native"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

namespace {
//----------------------------------------------------------------------------
    bool doDelaunay(vtkSmartPointer<vtkPolyData> source, vtkSmartPointer<vtkPolyData> target, int iterations
            , bool ascii, std::string pathStr){

            vtkSmartPointer<vtkCleanPolyData> cleaner = vtkSmartPointer<vtkCleanPolyData>::New();
            vtkSmartPointer<vtkDelaunay3D> delaunay3D = vtkSmartPointer<vtkDelaunay3D>::New();

            return true;

    }
    bool doIcp(vtkSmartPointer<vtkPolyData> source, vtkSmartPointer<vtkPolyData> target, int iterations
        , bool ascii, std::string pathStr){

        LOGI("Native doIcp. instantiating ICP.");
        vtkSmartPointer<vtkIterativeClosestPointTransform> icp =
            vtkSmartPointer<vtkIterativeClosestPointTransform>::New();
        LOGI("Native doIcp. ICP instance success.");

        LOGI("Native doIcp. Setup ICP transform.");
        icp->SetSource(source);
        icp->SetTarget(target);
        icp->GetLandmarkTransform()->SetModeToRigidBody();
        icp->SetMaximumNumberOfIterations(iterations);
        //icp->StartByMatchingCentroidsOn(); //Comentado desde el ejemplo
        icp->Modified();
        icp->Update();

        LOGI("Get the resulting transformation matrix (this matrix takes the source points to the target points)");
        vtkSmartPointer<vtkMatrix4x4> m = icp->GetMatrix();
//        std::cout << "The resulting matrix is: " << *m << std::endl;
        LOGI("The resulting matrix is: N/A");

        LOGI("Transform the source points by the ICP solution");
        vtkSmartPointer<vtkTransformPolyDataFilter> icpTransformFilter
            = vtkSmartPointer<vtkTransformPolyDataFilter>::New();
        #if VTK_MAJOR_VERSION <= 5
          icpTransformFilter->SetInput(source);
        #else
          icpTransformFilter->SetInputData(source);
        #endif
        icpTransformFilter->SetTransform(icp);
        icpTransformFilter->Update();

        std::string filenameStr = std::string(pathStr.c_str());
        std::string pointsStr ("test_uniform_target");
        std::string asciiStr (ascii? "_ascii" : "");
        std::string fileExtStr (".vtp");
        filenameStr = filenameStr + pointsStr + asciiStr + fileExtStr;

        vtkSmartPointer<vtkXMLPolyDataWriter> writer = vtkSmartPointer<vtkXMLPolyDataWriter>::New();
        writer->SetFileName(filenameStr.c_str());
        #if VTK_MAJOR_VERSION <= 5
          writer->SetInput(target);
        #else
          writer->SetInputData(target);
        #endif

        // Optional - set the mode. The default is binary.
        if(ascii){
            writer->SetDataModeToAscii();
        }

        writer->Write();

        vtkSmartPointer<vtkPolyDataMapper> solutionMapper = vtkSmartPointer<vtkPolyDataMapper>::New();
        solutionMapper->SetInputConnection(icpTransformFilter->GetOutputPort());

        vtkSmartPointer<vtkPolyData> polydata = vtkSmartPointer<vtkPolyData>::New();
        polydata = solutionMapper->GetInput();

        std::string outputFilenameStr = std::string(pathStr.c_str());
        std::string resultStr ("icp_result");
        filenameStr = outputFilenameStr + resultStr + asciiStr + fileExtStr;
        writer->SetFileName(outputFilenameStr.c_str());
        #if VTK_MAJOR_VERSION <= 5
          writer->SetInput(target);
        #else
          writer->SetInputData(target);
        #endif
        writer->Write();

        return true;
    }

    bool jsonToVTK(const std::string& data, const std::string& projectPathStr, const bool& ascii){
            Json::Value root;
            Json::Reader reader;
            bool parsingSuccessful = reader.parse(data, root);
            if ( !parsingSuccessful ) {
                std::cout  << "Failed to parse configuration\n" << reader.getFormattedErrorMessages();
                return false;
            }
            vtkSmartPointer<vtkPoints> points = vtkSmartPointer<vtkPoints>::New();
            for(int i = 0; i < root.size(); ++i){
                double x = root[i]["x"].asDouble();
                double y = root[i]["y"].asDouble();
                double z = root[i]["z"].asDouble();
                points->InsertNextPoint(x, y, z);
            }
            vtkSmartPointer<vtkPolyData> polydata = vtkSmartPointer<vtkPolyData>::New();
            polydata->SetPoints(points);

            LOGI("jsonVtk.projectPathStr: %s", projectPathStr.c_str());
            //set file path
            std::string filenameStr = std::string(projectPathStr.c_str());
            std::string pointsStr ("points");
            std::string asciiStr (ascii? "_ascii" : "");
            std::string fileExtStr (".vtp");
            filenameStr = filenameStr + pointsStr + asciiStr + fileExtStr;
            LOGI("jsonVtk.source: %s", filenameStr.c_str());

            vtkSmartPointer<vtkXMLPolyDataWriter> writer = vtkSmartPointer<vtkXMLPolyDataWriter>::New();
            writer->SetFileName(filenameStr.c_str());
            #if VTK_MAJOR_VERSION <= 5
              writer->SetInput(polydata);
            #else
              writer->SetInputData(polydata);
            #endif
            if(ascii){
                writer->SetDataModeToAscii();
            }
            writer->Write();

            std::string targetFilenameStr = std::string(projectPathStr.c_str());
            std::string targetStr ("target");
            targetFilenameStr = targetFilenameStr + targetStr + asciiStr + fileExtStr;
            LOGI("jsonVtk.target: %s", targetFilenameStr.c_str());

            vtkSmartPointer<vtkXMLPolyDataReader> targetReader =
                  vtkSmartPointer<vtkXMLPolyDataReader>::New();
            targetReader->SetFileName(targetFilenameStr.c_str());
            targetReader->Update();
            vtkSmartPointer<vtkPolyData> target = vtkSmartPointer<vtkPolyData>::New();
            target->ShallowCopy(targetReader->GetOutput());

//            doIcp(polydata, target, 20, ascii, projectPathStr);

            return true;
        }
};

//----------------------------------------------------------------------------
extern "C" {
  JNIEXPORT jboolean JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_jsonToVTK(JNIEnv * env
    , jobject obj, jstring data, jstring projectPathStr, jboolean ascii);
};

JNIEXPORT jboolean JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_jsonToVTK(JNIEnv * env
    , jobject obj,  jstring data, jstring projectPathStr, jboolean ascii)
{
	const char *javaStr = env->GetStringUTFChars(data, NULL);
	const char *javaFilenameStr = env->GetStringUTFChars(projectPathStr, NULL);
	const bool booleanField = ascii;
	LOGI("JNIExport Testing jsonToVTK: %s", javaFilenameStr);

        //TODO FIX
	  if (javaStr) {
		std::string dataStr = javaStr;
		std::string filenameStr = javaFilenameStr;
		return jsonToVTK(dataStr, filenameStr, booleanField);
	  }
	return false;
}
