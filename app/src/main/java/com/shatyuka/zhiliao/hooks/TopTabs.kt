package com.shatyuka.zhiliao.hooks

import com.shatyuka.zhiliao.Helper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.hookMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.setObjectField

/**
 * api: /root/tab
 */
class TopTabs : BaseHook() {

    private val PREFS_KEY_NAME = "edit_tabs"

    private var activityInfo: Class<*>? = null
    private var topTabs: Class<*>? = null
    private var tabInfo: Class<*>? = null
    private var tabStyle: Class<*>? = null
    private var textTab: Class<*>? = null

    override fun getName(): String {
        return "自定义首页顶部Tab(TopTabs)"
    }

    override fun init(classLoader: ClassLoader) {
        try {
            activityInfo = classLoader.loadClass("com.zhihu.android.api.model.ActivityInfo")
        } catch (e: Exception) {
            logE(e.message)
        }

        try {
            topTabs = classLoader.loadClass("com.zhihu.android.api.model.TopTabs")
        } catch (e: Exception) {
            logE(e.message)
        }

        try {
            tabInfo = classLoader.loadClass("com.zhihu.android.api.model.TopTabInfo")
        } catch (e: Exception) {
            logE(e.message)
        }

        try {
            tabStyle = classLoader.loadClass("com.zhihu.android.api.model.TabStyle")
        } catch (e: Exception) {
            logE(e.message)
        }

        try {
            textTab = classLoader.loadClass("com.zhihu.android.api.model.TextTab")
        } catch (e: Exception) {
            logE(e.message)
        }
    }

    override fun hook() {
        Helper.prefs.getBoolean("switch_mainswitch", false).let { if (!it) return }

        hookMethod(Helper.JacksonHelper.ObjectReader_readValue, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.result == null) return
                when (param.result.javaClass) {
                    activityInfo -> {
                        setObjectField(param.result, "topActivity", null)
                    }

                    topTabs -> {
                        postProcessTopTabs(param.result)
                    }
                }
            }
        })
    }

    private fun postProcessTopTabs(topTabs: Any) {
        setObjectField(topTabs, "topActivity", null)
        val tabConfig = Helper.prefs.getString(PREFS_KEY_NAME, "")!!
        if ("*" == tabConfig) {
            val fakeTabInfo = makeFakeTabInfo()
            XposedBridge.log(listOf(fakeTabInfo).toString())
            setObjectField(topTabs, "tabs", listOf(fakeTabInfo))
        } else {
            val tabList = getObjectField(topTabs, "tabs") as List<*>
            val originTabMap = tabList.associateBy { getObjectField(it, "tabType") as String }
            val allowedTabTypeList = tabConfig.split("|").map { it.trim() }
            val resultTabList = originTabMap.filterKeys { it in allowedTabTypeList }.values.toList()
            if (resultTabList.isNotEmpty()) {
                setObjectField(topTabs, "tabs", resultTabList)
            } else {
                resetAllowedTabTypeList(originTabMap.keys)
            }
        }
    }

    private fun makeFakeTabInfo(): Any? {
        val fakeTabInfo = tabInfo!!.constructors[0].newInstance()
        val fakeTabStyle = tabStyle!!.constructors[0].newInstance()
        val fakeTextTab = textTab!!.constructors[0].newInstance()
        setObjectField(fakeTextTab, "title", "ㅤ")
        setObjectField(fakeTextTab, "color", "#ffffff")
        setObjectField(fakeTextTab, "colorNight", "#000000")
        setObjectField(fakeTabStyle, "type", "text")
        setObjectField(fakeTabStyle, "text", fakeTextTab)
        setObjectField(fakeTabInfo, "tabType", "activity")
        setObjectField(fakeTabInfo, "tabName", "")
        setObjectField(fakeTabInfo, "clickableInBrowseMode", false)
        setObjectField(fakeTabInfo, "moduleId", "")
        setObjectField(fakeTabInfo, "url", "https://www.zhihu.com/parker/")
        setObjectField(fakeTabInfo, "startTime", null)
        setObjectField(fakeTabInfo, "endTime", null)
        setObjectField(fakeTabInfo, "normal", fakeTabStyle)
        setObjectField(fakeTabInfo, "selected", fakeTabStyle)
        setObjectField(fakeTabInfo, "flipConfig", null)
        return fakeTabInfo
    }

    private fun resetAllowedTabTypeList(tabTypeList: Collection<String>) {
        Helper.prefs.edit().putString(PREFS_KEY_NAME, tabTypeList.joinToString { "|" }).apply()
    }
}