# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn com.gondroid.auth.presentation.LoginScreenKt
-dontwarn com.gondroid.auth.presentation.LoginScreenViewModel
-dontwarn com.gondroid.auth.presentation.LoginScreenViewModel_HiltModules$KeyModule
-dontwarn com.gondroid.configuration.presentation.ConfigurationScreenKt
-dontwarn com.gondroid.configuration.presentation.ConfigurationScreenViewModel
-dontwarn com.gondroid.configuration.presentation.ConfigurationScreenViewModel_HiltModules$KeyModule
-dontwarn com.gondroid.configuration.presentation.customize.CustomizeScreenKt
-dontwarn com.gondroid.configuration.presentation.customize.CustomizeScreenViewModel
-dontwarn com.gondroid.configuration.presentation.customize.CustomizeScreenViewModel_HiltModules$KeyModule
-dontwarn com.gondroid.configuration.presentation.term.TermScreenKt
-dontwarn com.gondroid.core.data.adapter.AuthProviderAdapter
-dontwarn com.gondroid.core.data.adapter.FacebookAuthAdapter
-dontwarn com.gondroid.core.data.adapter.GoogleAuthAdapter
-dontwarn com.gondroid.core.data.di.AuthProvidersModule_ProvideCredentialManagerFactory
-dontwarn com.gondroid.core.data.di.DataStoreModule_ProvideDataStoreFactory
-dontwarn com.gondroid.core.data.di.FirebaseModule_ProvideFirebaseAuthFactory
-dontwarn com.gondroid.core.data.di.RepositoryModule_ProvidePreferenceRepositoryFactory
-dontwarn com.gondroid.core.data.di.RepositoryModule_ProvideQuizRepositoryFactory
-dontwarn com.gondroid.core.data.repository.AuthRepositoryImpl
-dontwarn com.gondroid.core.database.dao.EvaluationDao
-dontwarn com.gondroid.core.database.di.DataModule_ProvideDataBaseFactory
-dontwarn com.gondroid.core.database.di.DataModule_ProvideEvaluationDaoFactory
-dontwarn com.gondroid.core.presentation.designsystem.ThemeKt
-dontwarn com.gondroid.core.presentation.ui.ConfigurationScreenRoute
-dontwarn com.gondroid.core.presentation.ui.CustomizeScreenRoute
-dontwarn com.gondroid.core.presentation.ui.DetailScreenRoute
-dontwarn com.gondroid.core.presentation.ui.EvaluationScreenRoute
-dontwarn com.gondroid.core.presentation.ui.HomeScreenRoute
-dontwarn com.gondroid.core.presentation.ui.LoginScreenRoute
-dontwarn com.gondroid.core.presentation.ui.PdfScreenRoute
-dontwarn com.gondroid.core.presentation.ui.QuestionsScreenRoute
-dontwarn com.gondroid.core.presentation.ui.SummaryScreenRoute
-dontwarn com.gondroid.core.presentation.ui.TermScreenRoute
-dontwarn com.gondroid.detail.presentation.DetailScreenKt
-dontwarn com.gondroid.detail.presentation.DetailScreenViewModel
-dontwarn com.gondroid.detail.presentation.DetailScreenViewModel_HiltModules$KeyModule
-dontwarn com.gondroid.evaluation.presentation.EvaluationScreenKt
-dontwarn com.gondroid.evaluation.presentation.EvaluationScreenViewModel
-dontwarn com.gondroid.evaluation.presentation.EvaluationScreenViewModel_HiltModules$KeyModule
-dontwarn com.gondroid.evaluation.presentation.summary.SummaryScreenKt
-dontwarn com.gondroid.evaluation.presentation.summary.SummaryScreenViewModel
-dontwarn com.gondroid.evaluation.presentation.summary.SummaryScreenViewModel_HiltModules$KeyModule
-dontwarn com.gondroid.home.presentation.HomeScreenKt
-dontwarn com.gondroid.home.presentation.HomeScreenViewModel
-dontwarn com.gondroid.home.presentation.HomeScreenViewModel_HiltModules$KeyModule
-dontwarn com.gondroid.pdf.presentation.PdfScreenKt
-dontwarn com.gondroid.pdf.presentation.PdfScreenViewModel
-dontwarn com.gondroid.pdf.presentation.PdfScreenViewModel_HiltModules$KeyModule
-dontwarn com.gondroid.questionreview.presentation.QuestionsScreenKt
-dontwarn com.gondroid.questionreview.presentation.QuestionsScreenViewModel
-dontwarn com.gondroid.questionreview.presentation.QuestionsScreenViewModel_HiltModules$KeyModule