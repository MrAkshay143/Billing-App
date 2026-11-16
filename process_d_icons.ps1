Add-Type -AssemblyName System.Drawing

$sourcePath = "C:\Users\aksha\.gemini\antigravity\brain\8263f366-5e94-473f-83da-7b1fa1457d8e\dokandaar_d_app_icon_centered_1794768816006.jpg"
$baseRes = "kotlin_billing_app\app\src\main\res"

$sourceBmp = New-Object System.Drawing.Bitmap($sourcePath)

# Densities and sizes
$densities = @{
    "mipmap-mdpi"    = @{ icon = 48;  fg = 108 }
    "mipmap-hdpi"    = @{ icon = 72;  fg = 162 }
    "mipmap-xhdpi"   = @{ icon = 96;  fg = 216 }
    "mipmap-xxhdpi"  = @{ icon = 144; fg = 324 }
    "mipmap-xxxhdpi" = @{ icon = 192; fg = 432 }
}

# 1. Update ic_launcher_background.xml to #FFFFFF
$bgXml = @"
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#FFFFFF</color>
</resources>
"@
$bgXml | Out-File -FilePath (Join-Path $baseRes "values\ic_launcher_background.xml") -Encoding utf8

# Helper to render high quality resized bitmap
function Get-ResizedBitmap {
    param([System.Drawing.Image]$src, [int]$w, [int]$h, [string]$mode)

    $target = New-Object System.Drawing.Bitmap($w, $h)
    $g = [System.Drawing.Graphics]::FromImage($target)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    if ($mode -eq "square") {
        $g.Clear([System.Drawing.Color]::White)
        $g.DrawImage($src, 0, 0, $w, $h)
    }
    elseif ($mode -eq "round") {
        $g.Clear([System.Drawing.Color]::Transparent)
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddEllipse(0, 0, $w, $h)
        $g.SetClip($path)
        $g.Clear([System.Drawing.Color]::White)
        $g.DrawImage($src, 0, 0, $w, $h)
    }
    elseif ($mode -eq "splash") {
        # White squircle with subtle border
        $g.Clear([System.Drawing.Color]::Transparent)
        $radius = [int]($w * 0.22)
        $d = $radius * 2
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddArc(0, 0, $d, $d, 180, 90)
        $path.AddArc($w - $d, 0, $d, $d, 270, 90)
        $path.AddArc($w - $d, $h - $d, $d, $d, 0, 90)
        $path.AddArc(0, $h - $d, $d, $d, 90, 90)
        $path.CloseFigure()
        $g.SetClip($path)
        $g.Clear([System.Drawing.Color]::White)
        $g.DrawImage($src, 0, 0, $w, $h)
    }
    elseif ($mode -eq "foreground") {
        # Transparent background, emblem scaled to fit the 66dp safe zone (approx 72% of 108dp)
        $g.Clear([System.Drawing.Color]::Transparent)
        # Draw source image directly filling the canvas since source already has white background with margins
        $g.DrawImage($src, 0, 0, $w, $h)

        # Make white pixels transparent with smooth threshold
        for ($y = 0; $y -lt $h; $y++) {
            for ($x = 0; $x -lt $w; $x++) {
                $p = $target.GetPixel($x, $y)
                if ($p.R -ge 245 -and $p.G -ge 245 -and $p.B -ge 245) {
                    $target.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 255, 255, 255))
                }
                elseif ($p.R -ge 220 -and $p.G -ge 220 -and $p.B -ge 220) {
                    # Feather edge
                    $alpha = [int](255 * (1.0 - ($p.R - 220) / 25.0))
                    $target.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($alpha, $p.R, $p.G, $p.B))
                }
            }
        }
    }
    elseif ($mode -eq "monochrome") {
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.DrawImage($src, 0, 0, $w, $h)
        for ($y = 0; $y -lt $h; $y++) {
            for ($x = 0; $x -lt $w; $x++) {
                $p = $target.GetPixel($x, $y)
                if ($p.R -ge 240 -and $p.G -ge 240 -and $p.B -ge 240) {
                    $target.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
                } else {
                    $lum = [int](0.299 * $p.R + 0.587 * $p.G + 0.114 * $p.B)
                    $alpha = [int](255 * (1.0 - $lum / 255.0))
                    $target.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($alpha, 0, 0, 0))
                }
            }
        }
    }

    $g.Dispose()
    return $target
}

foreach ($density in $densities.Keys) {
    $sizes = $densities[$density]
    $iconSize = $sizes.icon
    $fgSize = $sizes.fg
    $targetDir = Join-Path $baseRes $density

    Write-Host "Processing $density (Icon: ${iconSize}x${iconSize}, FG: ${fgSize}x${fgSize})..."

    $squareBmp = Get-ResizedBitmap -src $sourceBmp -w $iconSize -h $iconSize -mode "square"
    $squareBmp.Save((Join-Path $targetDir "ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $squareBmp.Dispose()

    $roundBmp = Get-ResizedBitmap -src $sourceBmp -w $iconSize -h $iconSize -mode "round"
    $roundBmp.Save((Join-Path $targetDir "ic_launcher_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $roundBmp.Dispose()

    $fgBmp = Get-ResizedBitmap -src $sourceBmp -w $fgSize -h $fgSize -mode "foreground"
    $fgBmp.Save((Join-Path $targetDir "ic_launcher_foreground.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $fgBmp.Dispose()

    $monoBmp = Get-ResizedBitmap -src $sourceBmp -w $fgSize -h $fgSize -mode "monochrome"
    $monoBmp.Save((Join-Path $targetDir "ic_launcher_monochrome.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $monoBmp.Dispose()
}

# 3. Generate splash logo (512x512)
Write-Host "Generating 512x512 splash logo..."
$splashBmp = Get-ResizedBitmap -src $sourceBmp -w 512 -h 512 -mode "splash"
$splashBmp.Save((Join-Path $baseRes "drawable\dokandaar_splash_logo.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$splashBmp.Dispose()

$sourceBmp.Dispose()
Write-Host "D-icon generation successfully completed!"
