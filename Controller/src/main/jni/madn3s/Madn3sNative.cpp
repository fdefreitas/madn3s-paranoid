
#include <jni.h>
#include <sys/types.h>
#include <android/log.h>

#include <cassert>
#include <fstream>
#include <iostream>
#include <sstream>
#include <string>

#include <json-cpp/json.h>
//#include <JSONCPP/json/json.h>

#include <vtkIterativeClosestPointTransform.h>
#include <vtkSmartPointer.h>
#include <vtkXMLPolyDataReader.h>
#include <vtkAlgorithmOutput.h>
#include <vtkVersion.h>
#include <vtkTransform.h>
#include <vtkVertexGlyphFilter.h>
#include <vtkPoints.h>
#include <vtkPolyData.h>
#include <vtkPolygon.h>
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

namespace patch
{
    template < typename T > std::string to_string( const T& n )
    {
        std::ostringstream stm ;
        stm << n ;
        return stm.str() ;
    }
}

namespace madn3s {
    std::string saveVtp(vtkSmartPointer<vtkPolyData> data, std::string projectPathStr, std::string fileName, bool ascii);
    std::string vtkMatrixToJson(vtkSmartPointer<vtkMatrix4x4> matrix);
    std::string jsonToVTK(const std::string& data, const std::string& projectPathStr, const std::string fileName, const bool& ascii, const bool& debug);

//----------------------------------------------------------------------------
    bool doDelaunay(vtkSmartPointer<vtkPolyData> source, vtkSmartPointer<vtkPolyData> target, int iterations
            , bool ascii, std::string pathStr){

            vtkSmartPointer<vtkCleanPolyData> cleaner = vtkSmartPointer<vtkCleanPolyData>::New();
            vtkSmartPointer<vtkDelaunay3D> delaunay3D = vtkSmartPointer<vtkDelaunay3D>::New();

            return true;

    }

    /**
    * Execute ICP algorythm
    */
    std::string doIcp(std::string strSource, std::string strTarget
            , int iterations, int landmarks, double maxMeanDistance, std::string projectPathStr, std::string fileName, bool ascii){

//            return example::testExample("/storage/emulated/0/Pictures/MADN3SController/scumbag-robin/example-icp-test.vtp");

            LOGI("Native doIcp. source %s.", strSource.c_str());
            LOGI("Native doIcp. target %s.", strTarget.c_str());
            LOGI("Native doIcp. iter %s.", patch::to_string(iterations).c_str());

            /* Load Source PolyData */
            vtkSmartPointer<vtkPolyData> source = vtkSmartPointer<vtkPolyData>::New();
            vtkSmartPointer<vtkXMLPolyDataReader> sourceReader = vtkSmartPointer<vtkXMLPolyDataReader>::New();
            sourceReader->SetFileName(strSource.c_str());
            sourceReader->Update();
            source->ShallowCopy(sourceReader->GetOutput());

            /* Load Target PolyData */
            vtkSmartPointer<vtkPolyData> target = vtkSmartPointer<vtkPolyData>::New();
            vtkSmartPointer<vtkXMLPolyDataReader> targetReader = vtkSmartPointer<vtkXMLPolyDataReader>::New();
            targetReader->SetFileName(strTarget.c_str());
            targetReader->Update();
            target->ShallowCopy(targetReader->GetOutput());

            /* Execute ICP */
            LOGI("Native doIcp. instantiating ICP.");
            vtkSmartPointer<vtkIterativeClosestPointTransform> icp =
                vtkSmartPointer<vtkIterativeClosestPointTransform>::New();
            LOGI("Native doIcp. ICP instance success.");

            LOGI("Native doIcp. Setup ICP transform.");
            icp->SetSource(source);
            LOGI("Native doIcp. Setup ICP transform. Source");
            icp->SetTarget(target);
            LOGI("Native doIcp. Setup ICP transform. Target");
            icp->GetLandmarkTransform()->SetModeToRigidBody();
            LOGI("Native doIcp. Setup ICP transform. RigidBody *slaps knee*");
            icp->SetMaximumNumberOfLandmarks(landmarks);
            icp->SetMaximumMeanDistance(maxMeanDistance);
            icp->SetMaximumNumberOfIterations(iterations);
            icp->SetMaximumNumberOfIterations(5);
//            LOGI("Native doIcp. Setup ICP transform. iterations");
            icp->StartByMatchingCentroidsOn(); //Comentado desde el ejemplo
            icp->Modified();
            LOGI("Native doIcp. Setup ICP transform. Modified");
            icp->Update();
            LOGI("Native doIcp. Setup ICP transform. Update");

            LOGI("Get the resulting transformation matrix (this matrix takes the source points to the target points)");
            vtkSmartPointer<vtkMatrix4x4> m = icp->GetMatrix();
            LOGI("The resulting matrix is: N/A");

            return vtkMatrixToJson(m);
    }

    /**
    * Transforms a vtkMatrix4x4 to String in the format: [672.2618351846742, 0, 359.5; 0, 672.2618351846742, 239.5; 0, 0, 1]
    * @return std::string matrix string
    */
    std::string vtkMatrixToJson(vtkSmartPointer<vtkMatrix4x4> matrix){
        std::ostringstream matrixStr;
        matrixStr << "[";
        for (int i = 0; i < 4; i++){
            for (int j = 0; j < 4; j++){
                matrixStr << matrix->GetElement(i,j);
                if(j < 3) { matrixStr << ","; }
                LOGI("Matrix element: %4f", matrix->GetElement(i,j));
            }
            if(i < 3) { matrixStr << ";"; }
        }
        matrixStr << "]";
        return matrixStr.str();
    }

    std::string jsonToVTK(const std::string& data, const std::string& projectPathStr, const std::string fileName, const bool& ascii, const bool& debug){
        Json::Value root;
        Json::Reader reader;
        //Parsear JSON
        bool parsingSuccessful = reader.parse(data, root);
        if ( !parsingSuccessful ) {
            std::cout  << "Failed to parse configuration\n" << reader.getFormattedErrorMessages();
            return NULL;
        }

        int numberOfPoints = root.size();
        //Extraer puntos de JSON (root)
        vtkSmartPointer<vtkPoints> points = vtkSmartPointer<vtkPoints>::New();
        for(int i = 0; i < numberOfPoints; ++i){
            double x = root[i]["x"].asDouble();
            double y = root[i]["y"].asDouble();
            double z = root[i]["z"].asDouble();
            points->InsertNextPoint(x, y, z);
        }

        //Almacenar puntos en polydata
        vtkSmartPointer<vtkPolyData> polydata = vtkSmartPointer<vtkPolyData>::New();

        //New stuff from http://vtk.1045678.n5.nabble.com/Polygon-triangulation-using-vtkPolugon-Triangulate-td1238179.html
//        LOGI("Creating Polygon");
//        vtkSmartPointer<vtkPolygon> polygon = vtkSmartPointer<vtkPolygon>::New();
//
//        LOGI("Setting number of polygon points");
//        points ->SetNumberOfPoints(numberOfPoints);
//
//        LOGI("Setting polygon points ids");
//        polygon->GetPointIds()->SetNumberOfIds(numberOfPoints);
//        for(int i = 0; i < numberOfPoints; ++i){
//            polygon->GetPointIds()->SetId(i, i);
//        }
//
//        LOGI("polydata allocate");
//        polydata->Allocate();
//        LOGI("polydata insert next cell");
//        polydata->InsertNextCell(polygon->GetCellType(), polygon->GetPointIds());
//        LOGI("polydata set points");
        polydata->SetPoints(points);

//        vtkSmartPointer<vtkPolyData> temp = vtkSmartPointer<vtkPolyData>::New();
//        temp->SetPoints(points);

        vtkSmartPointer<vtkVertexGlyphFilter> vertexFilter = vtkSmartPointer<vtkVertexGlyphFilter>::New();
        vertexFilter->AddInputData(polydata);
        vertexFilter->Update();
        polydata->ShallowCopy(vertexFilter->GetOutput());

//        if(debug){
        return saveVtp(polydata, projectPathStr, fileName, ascii);
//        }
//        return polydata;
    }

    std::string saveVtp(vtkSmartPointer<vtkPolyData> data, std::string projectPathStr, std::string fileName, bool ascii){

        std::string asciiStr (ascii? "_ascii" : "");
        std::string fileExtStr (".vtp");
        std::string filenameStr = projectPathStr + fileName + asciiStr + fileExtStr;
        LOGI("saveVtp.filePath: %s", filenameStr.c_str());

        vtkSmartPointer<vtkXMLPolyDataWriter> writer = vtkSmartPointer<vtkXMLPolyDataWriter>::New();
        writer->SetFileName(filenameStr.c_str());
        writer->SetInputData(data);

        if(ascii){ writer->SetDataModeToAscii(); }

        writer->Write();

        return filenameStr;
    }
};

//----------------------------------------------------------------------------
extern "C" {

    JNIEXPORT jstring JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_doIcp(JNIEnv * env,
        jobject obj, jstring sourceJson, jstring targetJson, jstring projectPath,
        jint currentIteration, jboolean ascii, jint nLandmarks,
        jdouble maxMeanDistance, jint nIterations, jboolean debug);

    JNIEXPORT jboolean JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_doDelaunay(JNIEnv * env,
        jobject obj, jstring icpFilePath, jdouble alpha);

    JNIEXPORT jstring JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_saveVtp(JNIEnv * env
        , jobject obj, jstring data, jstring path, jstring name);

};

JNIEXPORT jstring JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_doIcp(JNIEnv * env,
    jobject obj, jstring sourceJson, jstring targetJson, jstring projectPath, jint currentIteration,
    jboolean ascii, jint nLandmarks, jdouble maxMeanDistance, jint nIterations, jboolean debug) {

    LOGI("JNIEXPORT doIcp");
    const bool mDebug = debug;
    const int mCurrentIteration = currentIteration;
    const bool mAscii = ascii;
    const int mLandmarks = nLandmarks;
    const double mMaxMeanDistance = maxMeanDistance;
    const int mIterations = nIterations;
	const char *javaProjectPathStr = env->GetStringUTFChars(projectPath, NULL);

    std::string mProjectPathStr (javaProjectPathStr);
    std::string icpTransformMatrixStr;
    std::string dataStr;
    std::string filename;

    LOGI("doIcp JNI. procesando source");
    const char *sourceRawString = env->GetStringUTFChars(sourceJson, 0);
    LOGI("Source Json Str source: %s", sourceRawString);
    dataStr = sourceRawString;
    std::ostringstream filenameStream;
    filenameStream << mCurrentIteration << "_source";
    LOGI("filename source: %s", filenameStream.str().c_str());
    std::string sourceStr = madn3s::jsonToVTK(dataStr, mProjectPathStr, filenameStream.str()
        , mAscii, mDebug);
    env->ReleaseStringUTFChars(sourceJson, sourceRawString);

    LOGI("doIcp JNI. procesando target");
    const char *targetRawString = env-> GetStringUTFChars(targetJson, 0);
    dataStr = targetRawString;

    //clearing stream
    filenameStream.str("");
    filenameStream.clear();

    filenameStream << mCurrentIteration << "_target";
    LOGI("filename target: %s", filename.c_str());
    std::string targetStr = madn3s::jsonToVTK(dataStr, mProjectPathStr, filenameStream.str()
        , mAscii, mDebug);
    env->ReleaseStringUTFChars(targetJson, targetRawString);

    LOGI("doIcp JNI. do ICP for iteration %d", mCurrentIteration);
    icpTransformMatrixStr = madn3s::doIcp(sourceStr, targetStr, mIterations, mLandmarks, mMaxMeanDistance,
        mProjectPathStr, ("icp_" + mCurrentIteration), mAscii);

	return env->NewStringUTF(icpTransformMatrixStr.c_str());
}

JNIEXPORT jboolean JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_doDelaunay(JNIEnv * env
    , jobject obj, jstring icpFilePath, jdouble alpha){

    LOGI("doIcp JNI. doDelaunay with p0 and p1");

    return false;
}

JNIEXPORT jstring JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_saveVtp(JNIEnv * env
    , jobject obj, jstring data, jstring path, jstring name){

    const char *dataPointer = env->GetStringUTFChars(data, NULL);
    std::string dataStr = dataPointer;

    const char *pathPointer = env->GetStringUTFChars(path, NULL);
    std::string pathStr = pathPointer;

    const char *namePointer = env->GetStringUTFChars(name, NULL);
    std::string nameStr = namePointer;

    std::string resultStr = madn3s::jsonToVTK(dataStr, pathStr, nameStr, true, false);

    return env->NewStringUTF(resultStr.c_str());
}
