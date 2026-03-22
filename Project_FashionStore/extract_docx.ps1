Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead("d:\HSF302\G05-FashionStore\Project_FashionStore\RDS.docx")
$entry = $zip.GetEntry("word/document.xml")
$reader = new-object System.IO.StreamReader($entry.Open())
$xmlString = $reader.ReadToEnd()
$reader.Close()
$zip.Dispose()

$xml = [xml]$xmlString
$ns = new-object System.Xml.XmlNamespaceManager($xml.NameTable)
$ns.AddNamespace("w", "http://schemas.openxmlformats.org/wordprocessingml/2006/main")

$paragraphs = $xml.SelectNodes("//w:p", $ns)
$lines = foreach ($p in $paragraphs) {
    $texts = $p.SelectNodes(".//w:t", $ns)
    if ($texts) {
        ($texts | ForEach-Object { $_.InnerText }) -join ""
    }
}

$lines | Out-File -FilePath "d:\HSF302\G05-FashionStore\Project_FashionStore\RDS_text.txt"
