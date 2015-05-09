
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

namespace example {

    void CreatePolyData(vtkSmartPointer<vtkPolyData> polydata){
      // This function creates a set of 4 points (the origin and a point unit distance along each axis)

        vtkSmartPointer<vtkPoints> points =
        vtkSmartPointer<vtkPoints>::New();

        // Create points
        double origin[3] = {0.0, 0.0, 0.0};
        points->InsertNextPoint(origin);
        double p1[3] = {1.0, 0.0, 0.0};
        points->InsertNextPoint(p1);
        double p2[3] = {0.0, 1.0, 0.0};
        points->InsertNextPoint(p2);
        double p3[3] = {0.0, 0.0, 1.0};
        points->InsertNextPoint(p3);

        vtkSmartPointer<vtkPolyData> temp =
        vtkSmartPointer<vtkPolyData>::New();
        temp->SetPoints(points);

        vtkSmartPointer<vtkVertexGlyphFilter> vertexFilter =
        vtkSmartPointer<vtkVertexGlyphFilter>::New();
        #if VTK_MAJOR_VERSION <= 5
            vertexFilter->SetInputConnection(temp->GetProducerPort());
        #else
            vertexFilter->SetInputData(temp);
        #endif
        vertexFilter->Update();

        polydata->ShallowCopy(vertexFilter->GetOutput());
    }

    void PerturbPolyData(vtkSmartPointer<vtkPolyData> polydata){
        vtkSmartPointer<vtkPoints> points =
        vtkSmartPointer<vtkPoints>::New();
        points->ShallowCopy(polydata->GetPoints());

        for(vtkIdType i = 0; i < points->GetNumberOfPoints(); i++){
            double p[3];
            points->GetPoint(i, p);
            double perturb[3];
            if(i%3 == 0)
              {
              perturb[0] = .1; perturb[1] = 0; perturb[2] = 0;
              }
            else if(i%3 == 1)
              {
              perturb[0] = 0; perturb[1] = .1; perturb[2] = 0;
              }
            else
              {
              perturb[0] = 0; perturb[1] = 0; perturb[2] = .1;
              }

            for(unsigned int j = 0; j < 3; j++)
              {
              p[j] += perturb[j];
              }
            points->SetPoint(i, p);
        }

        polydata->SetPoints(points);
    }

    void TranslatePolyData(vtkSmartPointer<vtkPolyData> polydata) {
        vtkSmartPointer<vtkTransform> transform =
        vtkSmartPointer<vtkTransform>::New();
        transform->Translate(0,.3,0);

        vtkSmartPointer<vtkTransformPolyDataFilter> transformFilter =
        vtkSmartPointer<vtkTransformPolyDataFilter>::New();
        #if VTK_MAJOR_VERSION <= 5
            transformFilter->SetInputConnection(polydata->GetProducerPort());
        #else
            transformFilter->SetInputData(polydata);
        #endif
        transformFilter->SetTransform(transform);
        transformFilter->Update();

        polydata->ShallowCopy(transformFilter->GetOutput());
    }

    std::string testExample(std::string outputFilePath){
            vtkSmartPointer<vtkPolyData> source = vtkSmartPointer<vtkPolyData>::New();
            vtkSmartPointer<vtkPolyData> target = vtkSmartPointer<vtkPolyData>::New();

            std::cout << "Creating data..." << std::endl;
            CreatePolyData(source);
            target->ShallowCopy(source);
            TranslatePolyData(target);
            PerturbPolyData(target);

            // Setup ICP transform
            vtkSmartPointer<vtkIterativeClosestPointTransform> icp =
              vtkSmartPointer<vtkIterativeClosestPointTransform>::New();
            icp->SetSource(source);
            icp->SetTarget(target);
            icp->GetLandmarkTransform()->SetModeToRigidBody();
            icp->SetMaximumNumberOfIterations(20);
            //icp->StartByMatchingCentroidsOn();
            icp->Modified();
            icp->Update();

            // Get the resulting transformation matrix (this matrix takes the source points to the target points)
            vtkSmartPointer<vtkMatrix4x4> m = icp->GetMatrix();
            std::cout << "The resulting matrix is: " << *m << std::endl;

            // Transform the source points by the ICP solution
            vtkSmartPointer<vtkTransformPolyDataFilter> icpTransformFilter =
            vtkSmartPointer<vtkTransformPolyDataFilter>::New();
            icpTransformFilter->SetInputData(source);
            icpTransformFilter->SetTransform(icp);
            icpTransformFilter->Update();

            /*
            // If you need to take the target points to the source points, the matrix is:
            icp->Inverse();
            vtkSmartPointer<vtkMatrix4x4> minv = icp->GetMatrix();
            std::cout << "The resulting inverse matrix is: " << *minv << std::cout;
            */

    //        vtkSmartPointer<vtkPolyDataMapper> solutionMapper =
    //            vtkSmartPointer<vtkPolyDataMapper>::New();
    //        solutionMapper->SetInputConnection(icpTransformFilter->GetOutputPort());

            // Write the file
            vtkSmartPointer<vtkXMLPolyDataWriter> writer = vtkSmartPointer<vtkXMLPolyDataWriter>::New();
            writer->SetFileName(outputFilePath.c_str());
            writer->SetInputConnection(icpTransformFilter->GetOutputPort());
            writer->SetDataModeToAscii();

            writer->Write();

            return outputFilePath;
        }

}

namespace {
    std::string saveVtp(vtkSmartPointer<vtkPolyData> data, std::string projectPathStr, std::string fileName, bool ascii);

//----------------------------------------------------------------------------
    bool doDelaunay(vtkSmartPointer<vtkPolyData> source, vtkSmartPointer<vtkPolyData> target, int iterations
            , bool ascii, std::string pathStr){

            vtkSmartPointer<vtkCleanPolyData> cleaner = vtkSmartPointer<vtkCleanPolyData>::New();
            vtkSmartPointer<vtkDelaunay3D> delaunay3D = vtkSmartPointer<vtkDelaunay3D>::New();

            return true;

    }
    vtkSmartPointer<vtkPolyData> doIcp(vtkSmartPointer<vtkPolyData> source, vtkSmartPointer<vtkPolyData> target
        , int iterations, int landmarks, double maxMeanDistance){

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
        LOGI("Native doIcp. Setup ICP transform. iterations");
        //icp->StartByMatchingCentroidsOn(); //Comentado desde el ejemplo
        icp->Modified();
        LOGI("Native doIcp. Setup ICP transform. Modified");
        icp->Update();
        LOGI("Native doIcp. Setup ICP transform. Update");

//        LOGI("Get the resulting transformation matrix (this matrix takes the source points to the target points)");
//        vtkSmartPointer<vtkMatrix4x4> m = icp->GetMatrix();
//        LOGI("The resulting matrix is: N/A");

        LOGI("Transform the source points by the ICP solution");
        vtkSmartPointer<vtkTransformPolyDataFilter> icpTransformFilter
            = vtkSmartPointer<vtkTransformPolyDataFilter>::New();
        icpTransformFilter->SetInputData(source);
        icpTransformFilter->SetTransform(icp);
        icpTransformFilter->Update();
//
        vtkSmartPointer<vtkPolyDataMapper> solutionMapper = vtkSmartPointer<vtkPolyDataMapper>::New();
        solutionMapper->SetInputConnection(icpTransformFilter->GetOutputPort());

        vtkSmartPointer<vtkPolyData> polydata = vtkSmartPointer<vtkPolyData>::New();
        polydata = solutionMapper->GetInput();

        return polydata;
    }

    std::string doIcp(std::string strSource, std::string strTarget
            , int iterations, int landmarks, double maxMeanDistance, std::string projectPathStr, std::string fileName, bool ascii){

//            return example::testExample("/storage/emulated/0/Pictures/MADN3SController/scumbag-robin/example-icp-test.vtp");

            LOGI("Native doIcp. source %s.", strSource.c_str());
            LOGI("Native doIcp. target %s.", strTarget.c_str());
            LOGI("Native doIcp. iter %s.", patch::to_string(iterations).c_str());

            vtkSmartPointer<vtkPolyData> source = vtkSmartPointer<vtkPolyData>::New();
            vtkSmartPointer<vtkPolyData> target = vtkSmartPointer<vtkPolyData>::New();
            vtkSmartPointer<vtkXMLPolyDataReader> sourceReader = vtkSmartPointer<vtkXMLPolyDataReader>::New();
            sourceReader->SetFileName(strSource.c_str());
            sourceReader->Update();
            source->ShallowCopy(sourceReader->GetOutput());
            vtkSmartPointer<vtkXMLPolyDataReader> targetReader = vtkSmartPointer<vtkXMLPolyDataReader>::New();
            targetReader->SetFileName(strTarget.c_str());
            targetReader->Update();
            target->ShallowCopy(targetReader->GetOutput());

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
            //icp->SetMaximumMeanDistance(maxMeanDistance);
            //icp->SetMaximumNumberOfIterations(iterations);
            icp->SetMaximumNumberOfIterations(5);
//            LOGI("Native doIcp. Setup ICP transform. iterations");
            //icp->StartByMatchingCentroidsOn(); //Comentado desde el ejemplo
            icp->Modified();
            LOGI("Native doIcp. Setup ICP transform. Modified");
            icp->Update();
            LOGI("Native doIcp. Setup ICP transform. Update");

            LOGI("Get the resulting transformation matrix (this matrix takes the source points to the target points)");
            vtkSmartPointer<vtkMatrix4x4> m = icp->GetMatrix();
            LOGI("The resulting matrix is: N/A");

            LOGI("Transform the source points by the ICP solution");
            vtkSmartPointer<vtkTransformPolyDataFilter> icpTransformFilter
                = vtkSmartPointer<vtkTransformPolyDataFilter>::New();
            icpTransformFilter->SetInputData(source);
            icpTransformFilter->SetTransform(icp);
            icpTransformFilter->Update();
            LOGI("Finished Transformation by the ICP solution");

            LOGI("Mapping the ICP solution");
            vtkSmartPointer<vtkPolyDataMapper> solutionMapper = vtkSmartPointer<vtkPolyDataMapper>::New();
            vtkSmartPointer<vtkAlgorithmOutput> transformOutput = vtkSmartPointer<vtkAlgorithmOutput>::New();
            LOGI("Getting output port");
            transformOutput = icpTransformFilter->GetOutputPort();
            LOGI("setting input connection");
            /* Here makes Boom! */

            solutionMapper->SetInputConnection(transformOutput);

            LOGI("Saving Mapped ICP solution to polydata");
            vtkSmartPointer<vtkPolyData> polydata = vtkSmartPointer<vtkPolyData>::New();
            polydata = solutionMapper->GetInput();

            return saveVtp(polydata, projectPathStr, fileName, ascii);
    }

//    vtkSmartPointer<vtkPolyData> jsonToVTK(const std::string& data, const std::string& projectPathStr, const std::string fileName, const bool& ascii, const bool& debug){
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

        #if VTK_MAJOR_VERSION <= 5
          writer->SetInput(data);
        #else
          writer->SetInputData(data);
        #endif

        if(ascii){
            writer->SetDataModeToAscii();
        }

        writer->Write();

        return filenameStr;
    }
};

//----------------------------------------------------------------------------
extern "C" {

    JNIEXPORT jboolean JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_doIcp(JNIEnv * env
    , jobject obj, jobjectArray pyrlksData, jstring projectPath, jboolean ascii, jint nLandmarks
    , jdouble maxMeanDistance, jint nIterations, jboolean debug);

    JNIEXPORT jboolean JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_doDelaunay(JNIEnv * env
    , jobject obj, jstring icpFilePath, jdouble alpha);

};

JNIEXPORT jboolean JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_doIcp(JNIEnv * env
    , jobject obj, jobjectArray pyrlksData, jstring projectPath, jboolean ascii, jint nLandmarks
    , jdouble maxMeanDistance, jint nIterations, jboolean debug)
{
    LOGI("MOTHERFUCKER 1");
    const bool mDebug = debug;
    const bool mAscii = ascii;
    const int mLandmarks = nLandmarks;
    const double mMaxMeanDistance = maxMeanDistance;
    const int mIterations = nIterations;
	const char *javaProjectPathStr = env->GetStringUTFChars(projectPath, NULL);

    std::string mProjectPathStr (javaProjectPathStr);

    vtkSmartPointer<vtkPolyData> acumPolydata = vtkSmartPointer<vtkPolyData>::New();
    vtkSmartPointer<vtkPolyData> pnPolydata = vtkSmartPointer<vtkPolyData>::New();
    std::string acumPolydataStr;
    std::string pnPolydataStr;

    //TODO garantizar que hayan al menos 3 frames desde Java
	//Parse pyrlksData
	int stringCount = env->GetArrayLength(pyrlksData);

    LOGI("doIcp JNI. procesando p0");
	jstring p0string = (jstring) env->GetObjectArrayElement(pyrlksData, 0);
    const char *p0RawString = env->GetStringUTFChars(p0string, 0);
    LOGI("MOTHERFUCKER p0 %s", p0RawString);
    std::string dataStr = p0RawString;
//    vtkSmartPointer<vtkPolyData> p0Polydata = jsonToVTK(dataStr, mProjectPathStr, "0", mAscii, mDebug);
    std::string p0Str = jsonToVTK(dataStr, mProjectPathStr, "0", mAscii, mDebug);
    env->ReleaseStringUTFChars(p0string, p0RawString);

    LOGI("doIcp JNI. procesando p1");
    jstring p1string = (jstring) env->GetObjectArrayElement(pyrlksData, 1);
    const char *p1RawString = env-> GetStringUTFChars(p1string, 0);
    dataStr = p1RawString;
//    vtkSmartPointer<vtkPolyData> p1Polydata = jsonToVTK(dataStr, mProjectPathStr, "1", mAscii, mDebug);
    std::string p1Str = jsonToVTK(dataStr, mProjectPathStr, "1", mAscii, mDebug);
    env->ReleaseStringUTFChars(p1string, p1RawString);

    //do ICP with p0 and p1
    LOGI("doIcp JNI. do ICP with p0 and p1");
//    acumPolydata = doIcp(p0Polydata, p1Polydata, mIterations, mLandmarks, mMaxMeanDistance);
    acumPolydataStr = doIcp(p0Str, p1Str, mIterations, mLandmarks, mMaxMeanDistance, mProjectPathStr, "icp_1", mAscii);

//    jstring pnString = NULL;
//    for (int i = 2; i < stringCount; ++i) {
//        LOGI("doIcp JNI. do ICP with acum and p%s", patch::to_string(i).c_str());
//        pnString = (jstring) env->GetObjectArrayElement(pyrlksData, i);
//        const char *pnRawString = env->GetStringUTFChars(pnString, 0);
//        dataStr = pnRawString;
//        pnPolydataStr = jsonToVTK(dataStr, mProjectPathStr, patch::to_string(i), mAscii, mDebug);
//        env->ReleaseStringUTFChars(p0string, p0RawString);
//
//        LOGI("doIcp JNI. do ICP with acum and p%s. Running ICP", patch::to_string(i).c_str());
//        acumPolydataStr = doIcp(acumPolydataStr, pnPolydataStr, mLandmarks, mMaxMeanDistance, mIterations
//            , mProjectPathStr, "icp_" + patch::to_string(i), mAscii);
//    }
////    saveVtp(acumPolydata, mProjectPathStr, "icp", mAscii);

	return true;
}

JNIEXPORT jboolean JNICALL Java_org_madn3s_controller_vtk_Madn3sNative_doDelaunay(JNIEnv * env
    , jobject obj, jstring icpFilePath, jdouble alpha){

    LOGI("doIcp JNI. doDelaunay with p0 and p1");

    return false;
}
