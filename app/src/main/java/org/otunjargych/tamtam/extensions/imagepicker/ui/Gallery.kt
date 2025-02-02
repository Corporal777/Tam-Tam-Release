package org.otunjargych.tamtam.extensions.imagepicker.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.SimpleItemAnimator
import org.otunjargych.tamtam.R
import org.otunjargych.tamtam.databinding.GalleryBinding
import org.otunjargych.tamtam.extensions.EXTRA_SETUP
import org.otunjargych.tamtam.extensions.RESULT_NAME
import org.otunjargych.tamtam.extensions.imagepicker.core.ImageLoader
import org.otunjargych.tamtam.extensions.imagepicker.core.ImageLoaderImpl
import org.otunjargych.tamtam.extensions.imagepicker.model.Image
import org.otunjargych.tamtam.extensions.imagepicker.model.SetUp
import org.otunjargych.tamtam.extensions.imagepicker.utils.GridSpacingItemDecoration
import org.otunjargych.tamtam.extensions.imagepicker.utils.PermissionUtil
import org.otunjargych.tamtam.extensions.imagepicker.utils.StringUtil
import org.otunjargych.tamtam.extensions.imagepicker.utils.toOptionCompat
import org.otunjargych.tamtam.extensions.toastMessage

internal class Gallery() : AppCompatActivity(), GalleryListener {

    private lateinit var binding: GalleryBinding
    private var mHandler: Handler? = null
    private var mRunnable: Runnable? = null

    companion object {
        private const val TAG = "ImagePickerView"
        private const val REQUEST_GALLERY = 1011
        private const val REQUEST_PERMISSION = 1013

        private const val MAXIMUM_SELECTION = 30

        fun starterIntent(context: Context, setup: SetUp?): Intent {
            return Intent(context, Gallery::class.java).apply {
                putExtra(EXTRA_SETUP, setup)
            }
        }
    }

    private val setUp by lazy {
        intent.getParcelableExtra<SetUp>(
            EXTRA_SETUP
        )
    }

    private var maxSize =
        MAXIMUM_SELECTION

    private val imageList = mutableListOf<Image>()
    private val selectedList = mutableListOf<Image>()
    private var selectedText = ""
    private var isSingle = false

    private val imageLoader: ImageLoader by lazy {
        ImageLoaderImpl(this)
    }

    private var resultName = RESULT_NAME
    override var isMultipleChecked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Toast.makeText(this, "Долгое нажатие для выбора картинки", Toast.LENGTH_SHORT).show()
        setUp?.let {
            maxSize = it.max
            resultName = it.name
            isSingle = it.single
            selectedText = it.title ?: return@let
        }
        initToolbar()

        binding.recyclerView.apply {
            adapter = ImagePickerAdapter(
                imageList,
                this@Gallery,
                isSingle
            )
            addItemDecoration(
                GridSpacingItemDecoration(
                    3,
                    1,
                    true
                )
            )
            setHasFixedSize(true)
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            postponeEnterTransition()
            viewTreeObserver.addOnPreDrawListener {
                startPostponedEnterTransition()
                true
            }
        }

        PermissionUtil.hasGalleryPermissionDenied(this) {
            if (it) {
                PermissionUtil.requestGalleryPermission(
                    this,
                    REQUEST_PERMISSION
                )
            } else {
                loadImages()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (permissions.size == 1 &&
                permissions[0] == Manifest.permission.READ_EXTERNAL_STORAGE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                loadImages()
            } else {
                Log.d("Missing Permission", "Check for permission")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        if (requestCode == REQUEST_GALLERY) {
            data?.data?.let { uri ->
                Log.d("uri", uri.toString())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!isSingle) {
            menuInflater.inflate(R.menu.gallery_toolbar_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.gallery_done -> {
                onGalleryDone()
                return false
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadImages() {
        imageLoader.load {
            imageList.addAll(it)
            (binding.recyclerView.adapter as ImagePickerAdapter).notifyDataSetChanged()
            binding.progressBar.isVisible = false
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun initToolbar() {
        setSupportActionBar(binding.toolBar)
        supportActionBar?.title = selectedText
        // set left icon , inflate menu
        binding.toolBar.apply {
            setNavigationIcon(R.drawable.ic_arrow_24dp)
        }

        // left icon click event
        binding.toolBar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun selectedList(): List<Uri>? {
        return imageList.filter { it.selected }.map { it.path }
    }

    private fun selectedImage(image: Image) {
        if (image.selected) {
            imageList.find { it == image }?.selected = false
            selectedList.remove(image)
        } else {
            if (selectedList.size < maxSize) {
                imageList.find { it == image }?.selected = true
                selectedList.add(image)
            } else {
                Log.d(
                    TAG, StringUtil.getStringRes(
                        this,
                        R.string.select_max_toast,
                        maxSize
                    )
                )
            }
        }
        isMultipleChecked = isImageMultipleSelected()
        (binding.recyclerView.adapter as ImagePickerAdapter).updateItem(image)
        toolbarText(selectedList.size)
    }

    private fun toolbarText(count: Int) {
        selectedText = if (isSingle) {
            setUp?.title ?: ""
        } else {
            if (count > 0) {
                "$count/6"
            } else {
                setUp?.title ?: ""
            }
        }
        binding.toolBar.title = selectedText
    }

    override fun onChecked(image: Image) {
        Log.d(TAG, "item Checked")
        selectedImage(image)
    }

    override fun onClick(view: View, image: Image) {
        Log.d(TAG, "item Clicked")
        startActivity(
            Detail.starterIntent(
                this,
                image
            ),
            toOptionCompat(view, R.id.item_image).toBundle()
        )
    }

    override fun onClick(image: Image) {
        selectedImage(image).also {
            onGalleryDone()
        }
    }

    private fun onGalleryDone() {
        selectedList()?.let { uris -> receiveImages(uris) }
    }

    private fun receiveImages(uris: List<Uri>) {
        toastMessage(this, "Загрузка фото...")
        val resultIntent = Intent().apply {
            putParcelableArrayListExtra(resultName, ArrayList(uris))
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()

    }

    private fun isImageMultipleSelected(): Boolean {
        return imageList.find { it.selected } != null
    }

}