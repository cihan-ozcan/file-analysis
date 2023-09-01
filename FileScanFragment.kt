package com.securebridgelink.sblink

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.AsyncTask
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import com.android.volley.DefaultRetryPolicy
import com.example.sblink.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
private lateinit var loadingSpinner: ProgressBar
private val handler = Handler(Looper.getMainLooper())
private val API_KEY = "lun92se35d1c8e07yrt692xde9f2dba8bjmj4e9s5db4da6b4g5kz8nx76778d10"
private const val READ_EXTERNAL_STORAGE_REQUEST_CODE = 101
private const val FILE_PICK_REQUEST_CODE = 102
private const val RANDOM = "1234567890"


class FileScanFragment : Fragment() {
    var filePathToScan: String? = null
    private var fileUri: Uri? = null
    private lateinit var idText: TextView
    private lateinit var sha256Text: TextView
    private lateinit var scannerName: TextView
    private lateinit var status_text: TextView
    private lateinit var loadingOverlay: FrameLayout
    private var idValue: String = ""
    private var sha256Value: String = ""
    private lateinit var idStorage: IdStorage
    private lateinit var mAdView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filescan, container, false)

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as AppCompatActivity?
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        val pieChart: PieChart = view.findViewById(R.id.pieChart)
        pieChart.visibility = View.INVISIBLE
        context?.let {
            MobileAds.initialize(it) {}

            mAdView = view.findViewById(R.id.adView)
            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
        }
        setDarkModeBackground()


        idStorage = IdStorage(requireContext())

        activity?.supportActionBar?.title = getString(R.string.file_analysis)


        val idInput: EditText = view.findViewById(R.id.id_input)
        val scanIdButton: ImageButton = view.findViewById(R.id.scan_id_button)

        idText = view.findViewById(R.id.id_text)
        sha256Text = view.findViewById(R.id.sha256_text)
        scannerName = view.findViewById(R.id.scanner_name)
        status_text = view.findViewById(R.id.status_text)
        loadingSpinner = view.findViewById(R.id.loading_spinner)
        loadingOverlay = view.findViewById(R.id.loading_overlay)

        view.findViewById<TextView>(R.id.id_text).visibility = View.GONE
        view.findViewById<TextView>(R.id.sha256_text).visibility = View.GONE
        view.findViewById<ViewGroup>(R.id.card2).visibility = View.GONE
        view.findViewById<ViewGroup>(R.id.card1).visibility = View.GONE
        view.findViewById<ViewGroup>(R.id.card2).visibility = View.GONE
        view.findViewById<ViewGroup>(R.id.card3).visibility = View.GONE

        val uploadButton: ImageButton = view.findViewById(R.id.upload_button)

        view.findViewById<ImageButton>(R.id.save_id_button).setOnClickListener {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentTime = sdf.format(Date())
            val fileName = fileUri?.lastPathSegment ?: "file"
            idStorage.saveId(idValue, currentTime, fileName)
            Toast.makeText(requireContext(), getString(R.string.id_saved_successfully), Toast.LENGTH_SHORT).show()
        }


        uploadButton.setOnClickListener {

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_EXTERNAL_STORAGE_REQUEST_CODE)
            } else {

                openFileChooser()
            }
        }


        scanIdButton.setOnClickListener {
            val id = idInput.text.toString()
            if (id.isNotEmpty()) {

                loadingOverlay.visibility = View.VISIBLE
                loadingSpinner.visibility = View.VISIBLE
                activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)


                startRetrievingResults(id)
            } else {
                Toast.makeText(context, getString(R.string.please_enter_valid_id), Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<ImageButton>(R.id.copy_id_button).setOnClickListener {
            val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("ID", idValue)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, getString(R.string.id_copied), Toast.LENGTH_SHORT).show()
        }

        view.findViewById<ImageButton>(R.id.copy_sha256_button).setOnClickListener {
            val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("SHA256", sha256Value)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, getString(R.string.sha256_copied), Toast.LENGTH_SHORT).show()
        }
        filePathToScan = arguments?.getString("filePathToScan")
        filePathToScan?.let { path ->
            val fileUri = Uri.fromFile(File(path))
            uploadAndScanFile(fileUri)
        }

        val retrieveRecentApksButton: ImageButton = view.findViewById(R.id.retrieve_recent_apks_button)

        if (android.os.Build.VERSION.SDK_INT > 28) {
            retrieveRecentApksButton.visibility = View.GONE
        } else {
            retrieveRecentApksButton.setOnClickListener {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_EXTERNAL_STORAGE_REQUEST_CODE)
                } else {

                    retrieveAndShowRecentAPKs()
                }
            }
        }

        val retrieveApkButton: ImageButton = view.findViewById(R.id.retrieve_apk)
        retrieveApkButton.setOnClickListener {
            showInstalledAppsDialog()
        }

    }
    private fun getInstalledApplications(): List<ApplicationInfo> {
        val packageManager = requireActivity().packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .sortedByDescending { packageManager.getPackageInfo(it.packageName, 0).firstInstallTime }
    }

    fun showInstalledAppsDialog() {
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.loading_applications))
        progressDialog.setCancelable(true)
        progressDialog.show()


        object : AsyncTask<Void, Void, List<ApplicationInfo>>() {
            override fun doInBackground(vararg params: Void?): List<ApplicationInfo> {
                return getInstalledApplications()
            }

            override fun onPostExecute(installedApps: List<ApplicationInfo>) {
                progressDialog.dismiss()
                val adapter = AppListAdapter(requireContext(), installedApps)

                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.installed_applications_title))
                    .setAdapter(adapter) { _, which ->
                        val selectedApp = installedApps[which]
                        val apkPath = selectedApp.sourceDir
                        val bundle = Bundle().apply {
                            putString("filePathToScan", apkPath)
                        }
                        findNavController().navigate(R.id.fileScanFragment, bundle)
                    }
                    .setNegativeButton(getString(R.string.cancel_button), null)
                    .show()
            }
        }.execute()
    }
    private fun retrieveAndShowRecentAPKs() {
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.loading_recent_apks))
        progressDialog.setCancelable(true)
        progressDialog.show()


        object : AsyncTask<Void, Void, List<ApkListAdapter.ApkItem>>() {
            override fun doInBackground(vararg params: Void?): List<ApkListAdapter.ApkItem> {
                val apks = getRecentAPKs()
                return apks.map { ApkListAdapter.ApkItem(it.name, it.absolutePath) }
            }

            override fun onPostExecute(apkItems: List<ApkListAdapter.ApkItem>) {
                progressDialog.dismiss()

                if (apkItems.isEmpty()) {
                    Toast.makeText(context, getString(R.string.no_recent_apks_found), Toast.LENGTH_SHORT).show()
                    return
                }

                val adapter = ApkListAdapter(requireContext(), apkItems)

                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.select_apk_title))
                    .setAdapter(adapter) { _, which ->
                        val selectedApkItem = apkItems[which]
                        val fileUri = Uri.fromFile(File(selectedApkItem.path))
                        uploadAndScanFile(fileUri)
                    }
                    .setNegativeButton(getString(R.string.cancel_button)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }.execute()
    }

    private fun getRecentAPKs(): List<File> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return downloadsDir.listFiles { _, name -> name.endsWith(".apk") }?.sortedByDescending { it.lastModified() }?.take(10) ?: listOf()
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.history -> {
                findNavController().navigate(R.id.action_fileScanFragment_to_historyFragment)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onResume() {
        super.onResume()
        setHasOptionsMenu(true)
    }
    data class DataPart(
        var fileName: String,
        var content: ByteArray,
        var type: String
    )
    abstract class VolleyFileUploadRequest(
        method: Int,
        url: String,
        private val listener: Response.Listener<JSONObject>,
        errorListener: Response.ErrorListener
    ) : JsonObjectRequest(method, url, null, listener, errorListener) {
        override fun getBodyContentType(): String {
            return "multipart/form-data;boundary=$BOUNDARY"
        }
        override fun getBody(): ByteArray {
            val bos = ByteArrayOutputStream()
            val dataOutputStream = DataOutputStream(bos)

            dataOutputStream.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END)
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"scan_type\"$LINE_END")
            dataOutputStream.writeBytes(LINE_END)
            dataOutputStream.writeBytes("all")
            dataOutputStream.writeBytes(LINE_END)

            val byteData = getByteData()
            byteData.forEach { (key, dataPart) ->
                writeFile(key, dataPart, dataOutputStream)
            }
            dataOutputStream.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END)
            return bos.toByteArray()
        }
        abstract fun getByteData(): Map<String, DataPart>
        private fun writeFile(
            key: String,
            dataPart: DataPart,
            dataOutputStream: DataOutputStream
        ) {
            dataOutputStream.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END)
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"$key\"; filename=\"${dataPart.fileName}\"$LINE_END")
            dataOutputStream.writeBytes("Content-Type: ${dataPart.type}$LINE_END")
            dataOutputStream.writeBytes(LINE_END)

            val fileInputStream = ByteArrayInputStream(dataPart.content)
            var bytesAvailable = fileInputStream.available()

            val bufferSize = min(bytesAvailable, MAX_BUFFER_SIZE)

            val buffer = ByteArray(bufferSize)

            var bytesRead = fileInputStream.read(buffer, 0, bufferSize)

            while (bytesRead > 0) {
                dataOutputStream.write(buffer, 0, bufferSize)
                bytesAvailable = fileInputStream.available()
                val bufferSize = min(bytesAvailable, MAX_BUFFER_SIZE)

                bytesRead = fileInputStream.read(buffer, 0, bufferSize)
            }

            dataOutputStream.writeBytes(LINE_END)
        }

        companion object {
            private const val LINE_END = "\r\n"
            private const val TWO_HYPHENS = "--"
            private const val BOUNDARY = "VolleyBoundary$RANDOM"
            private const val MAX_BUFFER_SIZE = 1024 * 1024
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, FILE_PICK_REQUEST_CODE)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                openFileChooser()
            } else {

                Toast.makeText(context, getString(R.string.permission_required), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            fileUri = data?.data
            if (fileUri != null) {
                val fileNameWithPrefix = fileUri?.lastPathSegment ?: "file"
                val fileName = fileNameWithPrefix.replaceFirst("primary:", "")

                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle(getString(R.string.file_scanning_title))
                builder.setMessage(getString(R.string.file_scanning_message, fileName))
                builder.setPositiveButton(getString(R.string.button_yes)) { _, _ ->
                    uploadAndScanFile(fileUri!!)
                }
                builder.setNegativeButton(getString(R.string.button_no)) { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()

                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setTextColor(Color.parseColor("#0F2B46"))

                val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                negativeButton.setTextColor(Color.parseColor("#0F2B46"))
            }
        }
    }


    fun uploadAndScanFile(fileUri: Uri) {
        loadingOverlay.visibility = View.VISIBLE
        loadingSpinner.visibility = View.VISIBLE
        activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        val API_KEY = "lun92se35d1c8e07yrt692xde9f2dba8bjmj4e9s5db4da6b4g5kz8nx76778d10"

        val url = "https://hybrid-analysis.com/api/v2/quick-scan/file"
        val request = object : VolleyFileUploadRequest(
            com.android.volley.Request.Method.POST,
            url,
            Response.Listener { response ->
                val id = response.getString("id")
                startRetrievingResults(id)
            },
            Response.ErrorListener { error ->
                error.networkResponse?.let {
                    val statusCode = it.statusCode
                    val headers = it.headers
                    val data = String(it.data)
                    Log.e("FileUploadError", "Status Code: $statusCode, Error Detail: $data")
                } ?: run {
                    Log.e("FileUploadError", "File upload error: $error")
                }

            }


        ) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["accept"] = "application/json"
                params["user-agent"] = "Falcon Sandbox"
                params["api-key"] = API_KEY
                return params
            }


            override fun getByteData(): Map<String, DataPart> {
                val params = HashMap<String, DataPart>()
                val contentResolver = context?.contentResolver
                val inputStream = contentResolver?.openInputStream(fileUri)
                val bytes = inputStream?.readBytes() ?: ByteArray(0)
                val fileName = fileUri.lastPathSegment ?: "file"
                params["file"] = DataPart(fileName, bytes, "application/x-msdownload")
                return params
            }

            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["scan_type"] = "all"
                return params
            }
        }
        request.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(context).add(request)
    }

    fun startRetrievingResults(id: String) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                retrieveResults(id) { finished ->
                    if (finished) {

                        handler.removeCallbacksAndMessages(null)
                    } else {

                        handler.postDelayed(this, 10000)
                    }
                }
            }
        }
        handler.postDelayed(runnable, 10000)
    }



    fun retrieveResults(id: String, onResult: (Boolean) -> Unit) {
        val url = "https://hybrid-analysis.com/api/v2/quick-scan/$id"
        val request = object : JsonObjectRequest(
            Method.GET,
            url,
            null,
            Response.Listener { response ->
                Log.d("RetrieveResultsSuccess", "response: $response")


                val finished = response.getBoolean("finished")
                if (finished) {
                    val pieChart: PieChart? = view?.findViewById(R.id.pieChart)
                    pieChart?.visibility = View.VISIBLE
                    view?.findViewById<ImageButton>(R.id.copy_id_button)?.visibility = View.VISIBLE
                    view?.findViewById<ImageButton>(R.id.copy_sha256_button)?.visibility = View.VISIBLE
                    loadingOverlay.visibility = View.GONE
                    loadingSpinner.visibility = View.GONE
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                    val id = response.getString("id")
                    val sha256 = response.getString("sha256")


                    view?.let {
                        val scanners = response.getJSONObject("scanners_v2")


                        val totalPositives = scanners.getJSONObject("crowdstrike_ml").optDouble("positives", 0.0).toFloat() +
                                scanners.getJSONObject("metadefender").optDouble("positives", 0.0).toFloat() +
                                scanners.getJSONObject("virustotal").optDouble("positives", 0.0).toFloat()

                        val totalScans = scanners.getJSONObject("crowdstrike_ml").optDouble("total", 0.0).toFloat() +
                                scanners.getJSONObject("metadefender").optDouble("total", 0.0).toFloat() +
                                scanners.getJSONObject("virustotal").optDouble("total", 0.0).toFloat()


                        val totalNegatives = totalScans - totalPositives


                        val pieChartData = mapOf(
                            "Malicious" to totalPositives,
                            "Safe" to totalNegatives
                        )

                        val pieChart = it.findViewById<PieChart>(R.id.pieChart)
                        setupPieChart(pieChart, pieChartData)



                        val crowdStrikeScanner = scanners.getJSONObject("crowdstrike_ml")
                        if (crowdStrikeScanner.getString("status") != "no-result") {
                            val card1: ViewGroup = it.findViewById(R.id.card1)


                            var scannerName = crowdStrikeScanner.getString("name")
                            if (scannerName == "CrowdStrike Falcon Static Analysis (ML)") {
                                scannerName = "CrowdStrike"
                            }
                            card1.findViewById<TextView>(R.id.scanner_name).text = scannerName
                            card1.findViewById<TextView>(R.id.status_text).text = crowdStrikeScanner.getString("status")

                            val animation = AnimationUtils.loadAnimation(context,
                                R.anim.bounce_animation
                            )

                            card1.startAnimation(animation)
                            card1.visibility = View.VISIBLE
                            val crowdStrikeStatus = crowdStrikeScanner.getString("status")
                            processStatus(crowdStrikeStatus, card1.findViewById<TextView>(R.id.status_text))
                        } else {
                            it.findViewById<ViewGroup>(R.id.card1).visibility = View.GONE
                        }



                        val metadefenderScanner = scanners.getJSONObject("metadefender")
                        if (metadefenderScanner.getString("status") != "no-result") {
                            val card2: ViewGroup = it.findViewById(R.id.card2)
                            card2.findViewById<TextView>(R.id.scanner_name).text = metadefenderScanner.getString("name")
                            card2.findViewById<TextView>(R.id.status_text).text = metadefenderScanner.getString("status")
                            card2.findViewById<TextView>(R.id.total_text).text = getString(R.string.total_scans_value) + "${metadefenderScanner.getInt("total")}"
                            card2.findViewById<TextView>(R.id.positives_text).text = getString(R.string.threat_value) + "${metadefenderScanner.getInt("positives")}"
                            card2.findViewById<TextView>(R.id.percent_text).text = "%${metadefenderScanner.getInt("percent")}"


                            val animation = AnimationUtils.loadAnimation(context,
                                R.anim.bounce_animation
                            )

                            card2.startAnimation(animation)
                            card2.visibility = View.VISIBLE
                            val metadefenderStatus = metadefenderScanner.getString("status")
                            processStatus(metadefenderStatus, card2.findViewById<TextView>(R.id.status_text))

                        } else {
                            it.findViewById<ViewGroup>(R.id.card2).visibility = View.GONE
                        }


                        val virusTotalScanner = scanners.getJSONObject("virustotal")
                        if (virusTotalScanner.getString("status") != "no-result") {
                            val card3: ViewGroup = it.findViewById(R.id.card3)
                            card3.findViewById<TextView>(R.id.scanner_name).text = virusTotalScanner.getString("name")
                            card3.findViewById<TextView>(R.id.status_text).text = virusTotalScanner.getString("status")
                            card3.findViewById<TextView>(R.id.total_text).text = getString(R.string.total_scans_value) + "${virusTotalScanner.optInt("total", 0)}"
                            card3.findViewById<TextView>(R.id.positives_text).text =getString(R.string.threat_value) + "${virusTotalScanner.optInt("positives", 0)}"
                            card3.findViewById<TextView>(R.id.percent_text).text = "%${virusTotalScanner.optInt("percent", 0)}"

                            val animation = AnimationUtils.loadAnimation(context,
                                R.anim.bounce_animation
                            )

                            card3.startAnimation(animation)
                            card3.visibility = View.VISIBLE
                            val virusTotalStatus = virusTotalScanner.getString("status")
                            processStatus(virusTotalStatus, card3.findViewById<TextView>(R.id.status_text))

                        } else {
                            it.findViewById<ViewGroup>(R.id.card3).visibility = View.GONE
                        }
                    }


                    idValue = id
                    sha256Value = sha256
                    idText.text = "id = $id"
                    sha256Text.text = "sha256 = $sha256"
                    idText.visibility = View.VISIBLE
                    sha256Text.visibility = View.VISIBLE
                    loadingOverlay.visibility = View.GONE
                    view?.findViewById<ImageButton>(R.id.save_id_button)?.visibility = View.VISIBLE


                    onResult(true)
                } else {

                    onResult(false)
                }
            },
            Response.ErrorListener { error ->

                loadingOverlay.visibility = View.GONE
                loadingSpinner.visibility = View.GONE

                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                Toast.makeText(context, getString(R.string.invalid_id), Toast.LENGTH_LONG).show()


            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["accept"] = "application/json"
                headers["user-agent"] = "Falcon Sandbox"
                headers["api-key"] = API_KEY
                return headers
            }
        }


        Volley.newRequestQueue(context).add(request)
    }
    fun setupPieChart(pieChart: PieChart, data: Map<String, Float>) {
        val entries = ArrayList<PieEntry>()
        val total = data.values.sum()

        for ((key, value) in data) {
            val percentage = value / total * 100
            entries.add(PieEntry(percentage))
        }

        val dataSet = PieDataSet(entries, "")

        val COLORS = intArrayOf(Color.parseColor("#b30000"), Color.parseColor("#0F2B46"))
        dataSet.setColors(*COLORS)

        val legend = pieChart.legend
        legend.isEnabled = true
        legend.formSize = 12f
        legend.textSize = 14f
        legend.xEntrySpace = 10f
        legend.yEntrySpace = 5f

        val legendEntries = arrayOf(
            LegendEntry(getString(R.string.malicious), Legend.LegendForm.DEFAULT, 10f, 2f, null, COLORS[0]),
            LegendEntry(getString(R.string.safe), Legend.LegendForm.DEFAULT, 10f, 2f, null, COLORS[1])
        )
        legend.setCustom(legendEntries)

        dataSet.valueFormatter = PercentFormatter(pieChart)
        dataSet.valueTextColor = Color.WHITE

        dataSet.setDrawValues(true)
        dataSet.valueTextSize = 16f
        dataSet.label = null

        val pieData = PieData(dataSet)
        pieChart.data = pieData

        pieChart.setDrawEntryLabels(false)

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        pieChart.description.text = getString(R.string.scan_time) + "$currentTime"
        pieChart.description.textSize = 12f
        pieChart.description.textColor = Color.BLACK


        pieChart.description.xOffset = 0f
        pieChart.description.yOffset = -20f

        pieChart.invalidate()

    }


    fun processStatus(status: String, textView: TextView) {
        when (status) {
            "malicious" -> {
                textView.text = getString(R.string.malware_text)
                textView.setTextColor(Color.parseColor("#b30000"))
            }
            "clean" -> {
                textView.text = getString(R.string.clean_file_text)
                textView.setTextColor(Color.GREEN)
            }
            else -> {
                textView.text = status
                textView.setTextColor(Color.BLACK)
            }
        }
    }
    fun handleBackPressed(): Boolean {
        if (loadingSpinner.visibility == View.VISIBLE) {

            loadingOverlay.visibility = View.GONE
            loadingSpinner.visibility = View.GONE
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            Toast.makeText(context, getString(R.string.loading_cancelled_text), Toast.LENGTH_SHORT).show()
            return true
        } else {
            return false
        }
    }
    private fun setDarkModeBackground() {
        val sharedPreferences = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val darkMode = sharedPreferences.getBoolean("dark_mode", false)
        if (darkMode) {
            view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.darkBackgroundColor))
        } else {
            view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
    }


}




