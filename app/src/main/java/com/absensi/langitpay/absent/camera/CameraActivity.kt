package com.absensi.langitpay.absent.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.absensi.langitpay.R
import com.absensi.langitpay.abstraction.*
import com.bumptech.glide.Glide
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.size.SizeSelector
import com.otaliastudios.cameraview.size.SizeSelectors
import id.zelory.compressor.Compressor
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class CameraActivity : AppCompatActivity() {

    private val loader by lazy {
        loaderDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        camera.setLifecycleOwner(this)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            if (camera.isOpened){
               onBack()
            }else{
                btn_try_again.performClick()
            }
        }

        camera.setPictureSize(setSizeCamera())
        camera.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                super.onPictureTaken(result)

                loader.dismiss()
                val permissions = listOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                withPermissions(permissions,action = { _, deniedList ->
                    if (deniedList.isEmpty()){

                        val file = File.createTempFile("LangitPay", ".png")
                        result.toFile(file) {
                            Log.i("Image",file.absolutePath)
                            handlerResult(file)
                        }
                    }
                })
            }

            override fun onCameraError(exception: CameraException) {
                super.onCameraError(exception)
                loader.dismiss()
                onBack()
                exception.printStackTrace()
            }

        })

        btn_take_picture.setOnClickListener {
            visibility(
                cameraShow = true,
                linButtonShow = false,
                previewShow = false,
                btnTakePicture = true
            ) {
                camera.takePictureSnapshot()
                loader.show()
            }
        }
        btn_try_again.setOnClickListener {
            visibility(
                cameraShow = true,
                linButtonShow = false,
                previewShow = false,
                btnTakePicture = true
            )
        }
    }

    private fun handlerResult(result : File?){
        visibility(
            cameraShow = false,
            linButtonShow = true,
            previewShow = true,
            btnTakePicture = false
        ) {
            Glide.with(this).load(result).into(image_preview)
            if (result != null){
                GlobalScope.launch {
                    Compressor.compress(this@CameraActivity,result).apply {
                        btn_upload.setOnClickListener {
                            loader.show()
                            Handler().postDelayed({
                                loader.dismiss()
                                val intent = Intent()
                                intent.putExtra("image", result.absolutePath)
                                setResult(Activity.RESULT_OK, intent)
                                onBack()
                            },500)
                        }
                    }
                }
            }else{
                btn_upload.isEnabled = false
                btn_upload.setBackgroundResource(R.drawable.bg_button_unactive_rounded)
                toast("Terjadi Kesalahan Silahkan Ambil Photo Lagi")
            }
        }
    }
    private fun visibility(
        cameraShow: Boolean,
        linButtonShow: Boolean,
        previewShow: Boolean,
        btnTakePicture: Boolean,
        body: (() -> Unit)? = null
    ) {
        camera.visibilism(cameraShow)
        lin_button.visibilism(linButtonShow)
        image_preview.visibilism(previewShow)
        btn_take_picture.visibilism(btnTakePicture)
        Handler().postDelayed({
            body?.invoke()
        }, 200)
    }

    override fun onResume() {
        super.onResume()
        camera.open()
    }

    override fun onPause() {
        super.onPause()
        camera.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        camera.destroy()
    }

    private fun  setSizeCamera(): SizeSelector{
        return SizeSelectors.maxWidth(600)
    }
}
