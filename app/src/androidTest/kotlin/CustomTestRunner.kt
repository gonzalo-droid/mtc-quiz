import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Por defecto, Android utiliza AndroidJUnitRunner para las pruebas de instrumentación.
 * Sin embargo, cuando trabajamos con inyección de dependencias mediante Hilt,
 * necesitamos crear un Custom Test Runner:
 *
 * Este Custom Test Runner nos permitirá utilizar HiltTestApplication como base para nuestras pruebas,
 * facilitando la inyección de dependencias durante los tests.
 */
class CustomTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application? {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}