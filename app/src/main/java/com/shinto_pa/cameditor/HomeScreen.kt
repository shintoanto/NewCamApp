package com.shinto_pa.cameditor

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.shinto_pa.cameditor.Constants.REQUIRED_PERMISSION
import com.shinto_pa.cameditor.databinding.HomeScreenFragmentBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HomeScreen : Fragment() {

    private lateinit var viewBinding: HomeScreenFragmentBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outPutDirectoryFile: File
    private lateinit var cameraExecutor: ExecutorService

    // Rcode
//    private var selectedFile: Intent? = null
//    private lateinit var file: File
//    private lateinit var imageUri: Uri
//    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = HomeScreenFragmentBinding.inflate(inflater, container, false)

        outPutDirectoryFile = getOutputDirectory()
        Log.d("Res", "outputdirectory is $outPutDirectoryFile")

        // Request camera permissions
        if (allPermissionGranted()) startCamera() else ActivityCompat.requestPermissions(
            requireActivity(), REQUIRED_PERMISSION,
            Constants.REQUEST_CODE_PERMISSIONS
        )

        viewBinding.selfieBtn.setOnClickListener { takePhoto() }
        viewBinding.editorBtn.setOnClickListener { launchGalleryIntent() }

        cameraExecutor = Executors.newSingleThreadExecutor()
        lifecycleScope.launch {
            getBitmap()
            Log.d("Res----------///", getBitmap().toString())
        }

//        var getFiles =
//            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                if (result.resultCode == Activity.RESULT_OK) {
//                    val data: Intent? = result.data
//                    Log.i("resumeData", data.toString())
//                    viewBinding.viewFinder.setImageURI(data?.data)
//                    selectedFile = data
//                }
        return viewBinding.root
    }


//private fun handleOpenDocument(resultData: Intent?) {
//
//    if (resultData == null) {
//        return
//    }
//
//    sharedPreferences =
//        context?.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE)!!
//    resultData.data?.also { uri ->
//        uri.path?.let {
//            file = File(it)
//
//            val scaleDivider = 4
//            try {
//                // 1. Convert uri to bitmap
//                val fullBitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
//
//                // 2. Get the downsized image content as a byte[]
//                val scaleWidth = fullBitmap.width / scaleDivider
//                val scaleHeight = fullBitmap.height / scaleDivider
//                val compressedFile =
//                    getDownsizedImageBytes(fullBitmap, scaleWidth, scaleHeight)
//
//                imageUri = getImageUri(compressedFile!!)
//
//            } catch (ioEx: IOException) {
//                ioEx.printStackTrace();
//            }
//
//            contentResolver.query(resultData.data!!, null, null, null, null)
//        }?.use { cursor ->
//            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//            cursor.moveToFirst()
//            val imageName = cursor.getString(nameIndex)
////
////                Log.d("images","$imageName,, $imageUri,,  $uri,, ${file.path},, ${file.absoluteFile}")
////                Log.d("images2","$file,,  ${file.canonicalPath}")
////
////             val path =   uri.toString().substringBefore("/document")
////                Log.d("imagePathSlice", path)
//
//
//            val fileToUpload = MultipartBody.Part.createFormData(
//                "profilePicture",
//                imageName,
//                ContentUriRequestBody(contentResolver, imageUri)
//            )
//            Log.i("imageName", fileToUpload.toString())
//
//            val filename: RequestBody = file.name
//                .toRequestBody("application/jpeg".toMediaTypeOrNull())
//            Log.i("imageFile", filename.toString())
//
//            preferences?.getString("Authentication_Token", "")?.let { token ->
//                viewModel.postProfilePic(token = "Bearer $token", fileToUpload, filename)
//                Log.i("bannerProf", "$fileToUpload  $filename")
//            }
//        }
//    }

    private fun takePhoto(){
        val imageCapture = imageCapture ?: return
        val photofile = File(
            outPutDirectoryFile, SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.getDefault())
                .format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOption = ImageCapture.OutputFileOptions.Builder(photofile).build()
        imageCapture.takePicture(outputOption, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photofile)
                    val msg = "photo Saved"
                    Toast.makeText(requireContext(), "$msg $savedUri", Toast.LENGTH_LONG).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG, "onError:${exception.message}", exception)
                }
            }
        )
        val argOutput = outputOption as ImageCapture.OutputFileOptions
     //   Log.d("Res", argOutput.)
        if (outputOption != null) {
            val action = HomeScreenDirections.actionHomeScreenToEditScreen()
            Navigation.findNavController(requireView()).navigate(action)
        }

    }

    private suspend fun getBitmap(): Bitmap {
        val loading = ImageLoader(requireContext())
        val request = ImageRequest.Builder(requireContext())
            .data(takePhoto()).build()
        val result = (loading.execute(request) as SuccessResult).dataSource
        return (result as BitmapDrawable).bitmap
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also { mPreview ->
                    mPreview.setSurfaceProvider(
                        viewBinding.viewFinder.surfaceProvider
                    )
                }
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.d(Constants.TAG, "start camera fail")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionGranted() =
        REQUIRED_PERMISSION.all {
            ContextCompat.checkSelfPermission(
                requireContext(), it
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun getOutputDirectory(): File {
        val appContext = context?.applicationContext
        val mediaDir = Environment.getExternalStorageDirectory().let {
            File(it, resources.getString(R.string.app_name)).apply {
                mkdir()
                Log.d("Res", mkdir().toString())
            }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext?.filesDir!!
    }

    private fun launchGalleryIntent() {
        val intent = Intent(context, ImagePickerActivity::class.java)
        intent.putExtra(
            ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION,
            ImagePickerActivity.REQUEST_GALLERY_IMAGE
        )
        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true)
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1) // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1)
        startActivityForResult(intent, MainActivity.REQUEST_IMAGE)
    }

}

