package com.chartboost.sdk.internal.Libraries

import io.mockk.every
import io.mockk.spyk
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class CBUtilityTest {
    /* Test directory hierarchy used for the testing. The actual
     * hierarchy is mocked at the time of testing using listFiles()
         Chartboost
             --image
                 --image1.jpeg
                 --image2.jpeg
                 --image2Dir (Empty)
                 --image1Dir
                     --image1.jpeg
                     --image2.jpeg
                 --noMediaDir
                    --.nomedia
     */
    private val root = TestFile("Chartboost", true).get()
    private val imageDir = TestFile("image", true).get()
    private val image1Dir = TestFile("image1Dir", true).get()
    private val image2Dir = TestFile("image2Dir", true).get()
    private val noMediaDir = TestFile("noMediaDir", true).get()
    private val image1 = TestFile("image1.jpeg").get()
    private val image2 = TestFile("image2.jpeg").get()
    private val noMediaFile = TestFile(".nomedia").get()

    private data class TestFile(
        val name: String,
        private val isDirectory: Boolean = false,
    ) {
        fun get(): File {
            val file = spyk(File(name))
            every { file.isDirectory } returns isDirectory
            every { file.isFile } returns !isDirectory
            return file
        }
    }

    @Before
    fun setup() {
        every { root.listFiles() } returns null
        every { imageDir.listFiles() } returns null
        every { image1Dir.listFiles() } returns null
        every { image2Dir.listFiles() } returns null
        every { noMediaFile.listFiles() } returns null
    }

    @Test
    fun nullDirectoryListTest() {
        Assert.assertTrue(CBUtility.listFiles(null, true).isEmpty())
    }

    @Test
    fun emptyDirectoryTest() {
        every { root.listFiles() } returns null
        val fileList = CBUtility.listFiles(root, true)
        assertEquals(fileList.size, 0)
    }

    @Test
    fun dirWithSomeFilesTest() {
        val input = arrayOf(image1, image2)
        val expected = ArrayList(listOf(*input))
        every { root.listFiles() } returns input
        val actual = CBUtility.listFiles(root, true)
        assertEquals(actual, expected)
        assertEquals(actual.size, 2)
    }

    @Test
    fun dirWithEmptyDirectoriesTest_RecursiveOnOff() {
        val input = arrayOf(image1Dir, image2Dir)
        val expected = ArrayList<File>()
        every { root.listFiles() } returns input
        var actual = CBUtility.listFiles(root, true)
        assertEquals(actual, expected)
        assertEquals(actual.size, 0)
        actual = CBUtility.listFiles(root, false)
        assertEquals(actual, expected)
        assertEquals(actual.size, 0)
    }

    @Test
    fun dirWithFilesAndEmptyDirectoriesTest_RecursiveOn() {
        val input = arrayOf(image1Dir, image2Dir, image1, image2)
        val expected = ArrayList(listOf(image1, image2))
        every { imageDir.listFiles() } returns input
        val actual = CBUtility.listFiles(imageDir, true)
        assertEquals(actual, expected)
        assertEquals(actual.size, 2)
    }

    @Test
    fun dirWithFilesAndNonEmptyFoldersTest_RecursiveOnOff() {
        every { imageDir.listFiles() } returns arrayOf(image1Dir, image2Dir, image1, image2)
        every { image1Dir.listFiles() } returns arrayOf(image1, image2)
        var actual = CBUtility.listFiles(imageDir, true)
        var expected = ArrayList(listOf(image1, image2, image1, image2))
        assertEquals(actual, expected)
        assertEquals(actual.size, 4)
        actual = CBUtility.listFiles(imageDir, false)
        expected = ArrayList(listOf(image1, image2))
        assertEquals(actual, expected)
        assertEquals(actual.size, 2)
    }

    @Test
    fun noMediaFileTest_RecursiveOnOff() {
        every { imageDir.listFiles() } returns arrayOf(image1Dir, image2Dir, noMediaDir)
        every { image1Dir.listFiles() } returns arrayOf(noMediaFile)
        every { image2Dir.listFiles() } returns arrayOf(noMediaFile)
        every { noMediaDir.listFiles() } returns arrayOf(noMediaFile)
        var actual = CBUtility.listFiles(imageDir, true)
        val expected = ArrayList<File>()
        assertEquals(actual, expected)
        assertEquals(actual.size, 0)
        actual = CBUtility.listFiles(imageDir, false)
        assertEquals(actual, expected)
        assertEquals(actual.size, 0)
    }
}
