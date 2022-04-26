package jp.techacademy.shintaro.nakagawa.taskapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.android.synthetic.main.activity_add_category.*
import java.util.*

class AddCategory : AppCompatActivity() {

    private val cOnAddClickListener = View.OnClickListener {
        val category: String? = editCategory.text.toString()
        val cConfig = RealmConfiguration.Builder()
            .name("Category.realm")
            .schemaVersion(1)
            .build()
        val cRealm: Realm = Realm.getInstance(cConfig)
        var cCategory = Category()

        if (!category.isNullOrBlank()) {
            val selectItem = cRealm.where(Category::class.java).equalTo("category", category).findAll()
            val spinnerItem = cRealm.where(Category::class.java).findAll()

            if (selectItem.isNullOrEmpty()) {
                cRealm.beginTransaction()

                cCategory.category = category
                val identifier: Int =
                    if (spinnerItem.max("id") != null) {
                        spinnerItem.max("id")!!.toInt() + 1
                    } else {
                        1
                    }
                cCategory!!.id = identifier

                cRealm.copyToRealmOrUpdate(cCategory!!)
                cRealm.commitTransaction()

                cRealm.close()

                val intent = Intent(this, InputActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_category)

        addButton.setOnClickListener(cOnAddClickListener)

        cancelButton.setOnClickListener {
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }
    }
}