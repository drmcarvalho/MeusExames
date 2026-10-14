package com.example.meusexames.dal

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArmazenamentoArquivos(context: Context) {

    private val context = context.applicationContext

    suspend fun obterNome(origem: Uri): String = withContext(Dispatchers.IO) { nomeDe(origem) }

    suspend fun obterTipoMime(origem: Uri): String? =
        withContext(Dispatchers.IO) { tipoMimeDe(origem) }

    /** Tamanho de [origem] em bytes, ou null quando o provedor do arquivo não informa. */
    suspend fun obterTamanho(origem: Uri): Long? = withContext(Dispatchers.IO) {
        // Arquivo local (file://) não passa por provedor: é medido direto.
        if (origem.scheme == ContentResolver.SCHEME_FILE) {
            return@withContext origem.path?.let { File(it).length() }
        }
        context.contentResolver
            .query(origem, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                val coluna = cursor.getColumnIndex(OpenableColumns.SIZE)
                // Arquivo remoto pode vir sem tamanho: a coluna existe, mas está nula.
                if (coluna >= 0 && cursor.moveToFirst() && !cursor.isNull(coluna)) {
                    cursor.getLong(coluna)
                } else {
                    null
                }
            }
    }

    /**
     * Copia [origem] para a pasta de exames do app e devolve o caminho da cópia relativo a
     * [Context.getFilesDir]. É esse caminho que deve ser gravado: o absoluto pode mudar.
     *
     * @throws IOException se a origem não puder ser lida ou a cópia não puder ser gravada.
     */
    suspend fun copiar(origem: Uri): String = withContext(Dispatchers.IO) {
        val extensao = MimeTypeMap.getSingleton().getExtensionFromMimeType(tipoMimeDe(origem))
        val caminho = "$PASTA_EXAMES/${UUID.randomUUID()}" + extensao?.let { ".$it" }.orEmpty()
        val destino = File(context.filesDir, caminho)
        destino.parentFile?.mkdirs()
        try {
            val entrada = context.contentResolver.openInputStream(origem)
                ?: throw FileNotFoundException(origem.toString())
            entrada.use { leitura -> destino.outputStream().use { leitura.copyTo(it) } }
        } catch (e: IOException) {
            destino.delete()
            throw e
        }
        caminho
    }

    suspend fun excluir(caminho: String) {
        withContext(Dispatchers.IO) { File(context.filesDir, caminho).delete() }
    }

    /**
     * Devolve o endereço pelo qual outros apps conseguem ler o arquivo gravado em [caminho]. Para
     * eles o arquivo se chama [nomeExibido], e não o nome que a cópia tem dentro do app.
     */
    suspend fun obterUri(caminho: String, nomeExibido: String): Uri = withContext(Dispatchers.IO) {
        FileProvider.getUriForFile(
            context,
            context.packageName + SUFIXO_AUTORIDADE,
            File(context.filesDir, caminho),
            nomeExibido,
        )
    }

    private fun nomeDe(origem: Uri): String {
        val nome = context.contentResolver
            .query(origem, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val coluna = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (coluna >= 0 && cursor.moveToFirst()) cursor.getString(coluna) else null
            }
        return nome ?: origem.lastPathSegment ?: origem.toString()
    }

    // Nem todo provedor informa o tipo; nesse caso vale o da extensão do nome do arquivo.
    private fun tipoMimeDe(origem: Uri): String? =
        context.contentResolver.getType(origem)
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(nomeDe(origem).substringAfterLast('.', "").lowercase())

    private companion object {
        // A pasta e a autoridade se repetem em res/xml/caminhos_arquivos.xml e no provedor
        // declarado no manifesto: quem mudar aqui precisa mudar lá.
        const val PASTA_EXAMES = "exames"
        const val SUFIXO_AUTORIDADE = ".arquivos"
    }
}
