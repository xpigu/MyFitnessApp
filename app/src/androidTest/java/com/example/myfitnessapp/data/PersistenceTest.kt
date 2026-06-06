package com.example.myfitnessapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myfitnessapp.data.database.AppDatabase
import com.example.myfitnessapp.data.entity.DietRecord
import com.example.myfitnessapp.data.entity.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenceTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ============================================================
    // 饮食数据持久化测试
    // ============================================================
    @Test
    fun testDietRecordInsertAndRetrieve() = runBlocking {
        val dao = db.dietRecordDao()

        // 插入一条记录
        val record = DietRecord(
            mealType = "BREAKFAST",
            foodName = "鸡蛋",
            calories = 100,
            timestamp = System.currentTimeMillis(),
            date = "2026-06-06",
            notes = "测试"
        )
        val id = dao.insert(record)

        // 验证插入成功
        assert(id == 1L)

        // 查询总卡路里
        val totalCal = dao.getTotalCaloriesByDate("2026-06-06")
        assert(totalCal == 100) { "期望 100 卡, 实际 $totalCal" }
    }

    @Test
    fun testDietRecordMultipleInserts() = runBlocking {
        val dao = db.dietRecordDao()

        // 插入多条记录
        val record1 = DietRecord(
            mealType = "BREAKFAST",
            foodName = "鸡蛋",
            calories = 100,
            timestamp = System.currentTimeMillis(),
            date = "2026-06-06"
        )
        val record2 = DietRecord(
            mealType = "LUNCH",
            foodName = "米饭",
            calories = 200,
            timestamp = System.currentTimeMillis() + 1000,
            date = "2026-06-06"
        )

        dao.insert(record1)
        dao.insert(record2)

        // 验证总卡路里
        val totalCal = dao.getTotalCaloriesByDate("2026-06-06")
        assert(totalCal == 300) { "期望 300 卡, 实际 $totalCal" }
    }

    @Test
    fun testDietRecordDelete() = runBlocking {
        val dao = db.dietRecordDao()

        val record = DietRecord(
            mealType = "SNACK",
            foodName = "水果",
            calories = 50,
            timestamp = System.currentTimeMillis(),
            date = "2026-06-06"
        )

        dao.insert(record)
        assert(dao.getTotalCaloriesByDate("2026-06-06") == 50)

        // 删除记录
        dao.delete(record)
        assert(dao.getTotalCaloriesByDate("2026-06-06") == 0)
    }

    // ============================================================
    // 用户资料持久化测试
    // ============================================================
    @Test
    fun testUserProfileInsertAndRetrieve() = runBlocking {
        val dao = db.userProfileDao()

        val profile = UserProfile(
            id = 1L,
            username = "测试用户",
            bio = "我是测试",
            gender = "MALE",
            heightCm = 175,
            weightKg = 70.0,
            birthday = "2000-01-01",
            level = 5
        )

        val id = dao.insert(profile)
        assert(id == 1L)

        // 异步查询验证
        val retrieved = dao.getUserProfileSync()
        assert(retrieved != null) { "用户资料不应为空" }
        assert(retrieved?.username == "测试用户") { "用户名应为 '测试用户'" }
        assert(retrieved?.heightCm == 175) { "身高应为 175" }
        assert(retrieved?.weightKg == 70.0) { "体重应为 70.0" }
    }

    @Test
    fun testUserProfileUpdate() = runBlocking {
        val dao = db.userProfileDao()

        val profile = UserProfile(
            id = 1L,
            username = "原始用户名"
        )

        dao.insert(profile)

        // 更新资料
        val updated = profile.copy(
            username = "更新后的用户名",
            heightCm = 180
        )
        dao.update(updated)

        val retrieved = dao.getUserProfileSync()
        assert(retrieved?.username == "更新后的用户名") { "用户名应为 '更新后的用户名'" }
        assert(retrieved?.heightCm == 180) { "身高应为 180" }
    }
}
