package jp.techacademy.shintaro.nakagawa.taskapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import io.realm.RealmChangeListener
import io.realm.Sort
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import android.app.AlarmManager
import android.app.PendingIntent
import android.util.Log
import android.view.View
import android.widget.*
import io.realm.RealmConfiguration

const val EXTRA_TASK = "jp.techacademy.shintaro.nakagawa.taskapp.TASK"

class MainActivity : AppCompatActivity() {

    var isFirstOpened = true

    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    private val cConfig = RealmConfiguration.Builder()
                    .name("Category.realm")
                    .schemaVersion(1)
                    .build()
    private lateinit var cRealm: Realm
    private val cRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadSpinner()
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener {
            val intent = Intent(this, InputActivity::class.java)
            Log.d("kotlintest", "fabClick")
            startActivity(intent)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)
        cRealm = Realm.getInstance(cConfig)
        cRealm.addChangeListener(cRealmListener)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this)

        // ListViewをタップしたときの処理
        listView1.setOnItemClickListener { parent, _, position, _ ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            // タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            val builder = AlertDialog.Builder(this)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK"){_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

        reloadListView()
        reloadSpinner()
    }

    private fun reloadListView() {
        // Realmデータベースから、「すべてのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

        // 上記の結果を、TaskListとしてセットする
        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
        cRealm.close()
    }

    private fun searchCategory(categoryId: Int) {
        var taskRealmResults: Any? = null
        if (categoryId == 0) {
            taskRealmResults = mRealm.where(Task::class.java).findAll()
                .sort("date")
        } else {
            taskRealmResults = mRealm.where(Task::class.java).equalTo("categoryId", categoryId).findAll()
                .sort("date")
        }

        // 上記の結果を、TaskListとしてセットする
        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()

    }

    private fun reloadSpinner() {
        var spinnerItems = cRealm.where(Category::class.java).findAll()
        val category = Category()
        var spinnerArray = arrayListOf<String>()
        var cId: Category? = null

        if (spinnerItems.isNullOrEmpty()) {
            category.category = "All"
            category.id = 0
            cRealm.beginTransaction()
            cRealm.copyToRealmOrUpdate(category)
            cRealm.commitTransaction()
            isFirstOpened = false
        }

        if (isFirstOpened) {
            spinnerItems = cRealm.where(Category::class.java).findAll()
        }

        for (num in spinnerItems) {
            spinnerArray.add(num.category)
        }

        // ArrayAdapter
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerArray)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // spinner に adapter をセット
        category_spinner.adapter = adapter

        category_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            //　アイテムが選択された時
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?, position: Int, id: Long) {
                val spinnerParent = parent as Spinner
                val item = spinnerParent.selectedItem as String
                val cId = cRealm.where(Category::class.java).equalTo("category", item).findAll()
                searchCategory(cId.max("id")!!.toInt())
            }

            //　アイテムが選択されなかった
            override fun onNothingSelected(parent: AdapterView<*>?) {
                //
            }
        }
    }
}