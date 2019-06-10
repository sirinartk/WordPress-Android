package org.wordpress.android.ui.stats.refresh.lists.widget

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsViewsWidgetConfigureViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsViewsWidgetConfigureViewModel.Color.DARK
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsViewsWidgetConfigureViewModel.Color.LIGHT
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class ColorSelectionDialogFragment : AppCompatDialogFragment() {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: StatsViewsWidgetConfigureViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(StatsViewsWidgetConfigureViewModel::class.java)
        val alertDialogBuilder = AlertDialog.Builder(activity)
        val view = activity!!.layoutInflater.inflate(R.layout.stats_color_selector, null) as RadioGroup
        view.setOnCheckedChangeListener { _, checkedId ->
            checkedId.toColor()?.let { viewModel.colorClicked(it) }
        }
        alertDialogBuilder.setView(view)
        viewModel.settingsModel.observe(this, Observer {
            val updatedColor = it?.color
            val currentColor = view.checkedRadioButtonId.toColor()
            if (updatedColor != currentColor) {
                updatedColor?.let { view.check(updatedColor.toViewId()) }
            }
        })
        alertDialogBuilder.setTitle(R.string.stats_widget_select_color)

        alertDialogBuilder.setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
            dialog?.dismiss()
        }
        return alertDialogBuilder.create()
    }

    private fun Color.toViewId(): Int {
        return when (this) {
            LIGHT -> R.id.stats_widget_light_color
            DARK -> R.id.stats_widget_dark_color
        }
    }

    private fun Int.toColor(): Color? {
        return when (this) {
            R.id.stats_widget_light_color -> LIGHT
            R.id.stats_widget_dark_color -> DARK
            else -> null
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }
}