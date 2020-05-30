package com.meteocool

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.model.SliderPage


class OnboardingActivity : AppIntro() {

    companion object{
        const val IS_ONBOARD_COMPLETED = "is_onboard_completed"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sliderPage1 = SliderPage()
        sliderPage1.title = getString(R.string.onboarding_title1)
        sliderPage1.description = getString(R.string.onboarding_description1)
        sliderPage1.imageDrawable = R.drawable.volunteers_c

        val sliderPage2 = SliderPage()
        sliderPage2.title = getString(R.string.onboarding_title2)
        sliderPage2.description = getString(R.string.onboarding_description2)
        sliderPage2.imageDrawable = R.drawable.jacket
        sliderPage2.backgroundColor = ContextCompat.getColor(this, R.color.colorPrimary)
        sliderPage2.titleColor = ContextCompat.getColor(this, R.color.colorText)
        sliderPage2.descriptionColor = ContextCompat.getColor(this, R.color.colorText)

        val sliderPage3 = SliderPage()
        sliderPage3.title = getString(R.string.onboarding_title3)
        sliderPage3.description = getString(R.string.onboarding_description3)
        sliderPage3.imageDrawable = R.drawable.bell
        sliderPage3.backgroundColor = ContextCompat.getColor(this, R.color.colorPrimary)
        sliderPage3.titleColor = ContextCompat.getColor(this, R.color.colorText)
        sliderPage3.descriptionColor = ContextCompat.getColor(this, R.color.colorText)

        val sliderPage4 = SliderPage()
        sliderPage4.title = getString(R.string.onboarding_title4)
        sliderPage4.description = getString(R.string.onboarding_description4)
        sliderPage4.imageDrawable = R.drawable.maps_and_location
        sliderPage4.backgroundColor = ContextCompat.getColor(this, R.color.colorPrimary)
        sliderPage4.titleColor = ContextCompat.getColor(this, R.color.colorText)
        sliderPage4.descriptionColor = ContextCompat.getColor(this, R.color.colorText)

        val sliderPage5 = SliderPage()
        sliderPage5.title = getString(R.string.onboarding_title5)
        sliderPage5.description = getString(R.string.onboarding_description5)
        sliderPage5.imageDrawable = R.drawable.sun_rain_composit_4
        sliderPage5.backgroundColor = ContextCompat.getColor(this, R.color.colorPrimary)
        sliderPage5.titleColor = ContextCompat.getColor(this, R.color.colorText)
        sliderPage5.descriptionColor = ContextCompat.getColor(this, R.color.colorText)


        setBarColor(ContextCompat.getColor(this, R.color.cloudAccent))
        setSeparatorColor(ContextCompat.getColor(this, R.color.colorText))
        isButtonsEnabled = true

        addSlide(AppIntroFragment.newInstance(sliderPage1))
        addSlide(AppIntroFragment.newInstance(sliderPage2))
        addSlide(AppIntroFragment.newInstance(sliderPage3))
        addSlide(AppIntroFragment.newInstance(sliderPage4))
        addSlide(AppIntroFragment.newInstance(sliderPage5))

        askForPermissions(
            permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            slideNumber = 3,
            required = false)
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        PreferenceManager.getDefaultSharedPreferences(this).edit().apply {
            putBoolean(IS_ONBOARD_COMPLETED, true)
            apply()
        }
        startActivity(Intent(this, MeteocoolActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // user cannot just skip the intro once
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }
}
