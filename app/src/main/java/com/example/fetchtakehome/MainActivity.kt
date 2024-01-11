package com.example.fetchtakehome
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.fetchtakehome.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var lists: ArrayList<String>
    lateinit var listAdapter: ArrayAdapter<String>
    var mainHandler = Handler(Looper.getMainLooper())
    lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeList()
        binding.fetchDataButton.setOnClickListener { FetchData().start() }
    }

    private fun initializeList() {
        lists = ArrayList()
        val listAdapter1 = ArrayAdapter<String>(this, android.R.layout.simple_list_item_2, android.R.id.text2, lists)

        listAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_2, android.R.id.text1, lists) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val text1 = view.findViewById<View>(android.R.id.text1) as TextView
                val text2 = view.findViewById<View>(android.R.id.text2) as TextView
                text1.text = getString(R.string.list_num, (position+1))
                text2.text = lists[position]
                return view
            }
        }
        binding.lists.adapter = listAdapter
    }

    inner class FetchData: Thread() {
        private var data = ""
        private var itemList: HashMap<Int, HashMap<Int, String>> = HashMap()

        override fun run() {
            mainHandler.post {
                progressBar = findViewById(R.id.progress_Bar)
                progressBar.visibility = View.VISIBLE
            }

            try {
                val url = URL("https://fetch-hiring.s3.amazonaws.com/hiring.json")
                val httpURLConnection = url.openConnection() as HttpURLConnection
                val inputStream = httpURLConnection.inputStream
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                var line = bufferedReader.readLine()

                while(line != null) {
                    data += line
                    line = bufferedReader.readLine()
                }

                if(data.isNotEmpty()) {
                    val items = JSONArray(data)
                    itemList.clear()
                    for(i in 0..<items.length()) {
                        val item = items.getJSONObject(i)
                        val id = item.getInt("id")
                        val listId = item.getInt("listId")
                        val name: String = item.getString("name")

                        if(name != "null" && name != "") {
                            if(itemList.containsKey(listId)) {
                                itemList[listId]?.put(id, name)
                            } else {
                                itemList[listId] = hashMapOf(id to name)
                            }
                        }
                    }
                }

                for(i in 1..itemList.size) {
                    lists.add(itemList[i]?.toSortedMap()?.values.toString())
                }

            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch(e: JSONException) {
                e.printStackTrace()
            }

            mainHandler.post {
                if (progressBar.isShown) {
                    progressBar.visibility = View.INVISIBLE
                }
                listAdapter.notifyDataSetChanged()
            }

        }
    }
}