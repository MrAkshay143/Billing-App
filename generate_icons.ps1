Add-Type -AssemblyName System.Drawing

function Draw-DokandaarIcon {
    param(
        [int]$size,
        [string]$type, # "square", "round", "foreground", "monochrome", "splash"
        [string]$outputPath
    )

    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    $indigoDark = [System.Drawing.Color]::FromArgb(255, 30, 27, 75)   # #1E1B4B
    $indigoPrimary = [System.Drawing.Color]::FromArgb(255, 79, 70, 229) # #4F46E5
    $indigoLight = [System.Drawing.Color]::FromArgb(255, 99, 102, 241) # #6366F1
    $accentEmerald = [System.Drawing.Color]::FromArgb(255, 16, 185, 129) # #10B981
    $accentGold = [System.Drawing.Color]::FromArgb(255, 245, 158, 11) # #F59E0B
    $white = [System.Drawing.Color]::White

    if ($type -eq "square") {
        # Rounded rectangle background
        $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)
        $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
            $rect, $indigoPrimary, $indigoDark, [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal
        )
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $radius = [int]($size * 0.22)
        $d = $radius * 2
        $path.AddArc(0, 0, $d, $d, 180, 90)
        $path.AddArc($size - $d, 0, $d, $d, 270, 90)
        $path.AddArc($size - $d, $size - $d, $d, $d, 0, 90)
        $path.AddArc(0, $size - $d, $d, $d, 90, 90)
        $path.CloseFigure()
        $g.FillPath($brush, $path)
        $brush.Dispose()
    }
    elseif ($type -eq "round") {
        $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)
        $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
            $rect, $indigoPrimary, $indigoDark, [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal
        )
        $g.FillEllipse($brush, 0, 0, $size, $size)
        $brush.Dispose()
    }
    elseif ($type -eq "splash") {
        $rect = New-Object System.Drawing.Rectangle(0, 0, $size, $size)
        $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
            $rect, $indigoPrimary, $indigoDark, [System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal
        )
        $radius = [int]($size * 0.24)
        $d = $radius * 2
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddArc(0, 0, $d, $d, 180, 90)
        $path.AddArc($size - $d, 0, $d, $d, 270, 90)
        $path.AddArc($size - $d, $size - $d, $d, $d, 0, 90)
        $path.AddArc(0, $size - $d, $d, $d, 90, 90)
        $path.CloseFigure()
        $g.FillPath($brush, $path)
        $brush.Dispose()
    }
    # foreground and monochrome have transparent background ($g.Clear([System.Drawing.Color]::Transparent))

    # Determine emblem scale and center
    # In foreground (108dp canvas), safe zone is middle 66dp (~61%)
    $scale = if ($type -in @("foreground", "monochrome")) { 0.52 } else { 0.65 }
    $emblemSize = $size * $scale
    $ox = ($size - $emblemSize) / 2
    $oy = ($size - $emblemSize) / 2

    # Draw Emblem: Shopping Bag + POS Barcode & Scanner Laser
    $bagBrush = if ($type -eq "monochrome") { New-Object System.Drawing.SolidBrush($white) } else { New-Object System.Drawing.SolidBrush($white) }
    $laserPen = if ($type -eq "monochrome") { New-Object System.Drawing.Pen($white, [Math]::Max(2, [int]($size * 0.035))) } else { New-Object System.Drawing.Pen($accentEmerald, [Math]::Max(2, [int]($size * 0.035))) }
    $goldPen = if ($type -eq "monochrome") { New-Object System.Drawing.Pen($white, [Math]::Max(2, [int]($size * 0.03))) } else { New-Object System.Drawing.Pen($accentGold, [Math]::Max(2, [int]($size * 0.03))) }
    $accentBrush = if ($type -eq "monochrome") { New-Object System.Drawing.SolidBrush($white) } else { New-Object System.Drawing.SolidBrush($accentEmerald) }

    # 1. Shopping Bag Body / Terminal Body (Rounded rect)
    $bWidth = $emblemSize * 0.78
    $bHeight = $emblemSize * 0.72
    $bx = $ox + ($emblemSize - $bWidth) / 2
    $by = $oy + $emblemSize * 0.28
    $bradius = [int]($bWidth * 0.16)
    $bd = $bradius * 2

    $bagPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $bagPath.AddArc($bx, $by, $bd, $bd, 180, 90)
    $bagPath.AddArc($bx + $bWidth - $bd, $by, $bd, $bd, 270, 90)
    $bagPath.AddArc($bx + $bWidth - $bd, $by + $bHeight - $bd, $bd, $bd, 0, 90)
    $bagPath.AddArc($bx, $by + $bHeight - $bd, $bd, $bd, 90, 90)
    $bagPath.CloseFigure()

    if ($type -eq "monochrome") {
        $g.FillPath($bagBrush, $bagPath)
    } else {
        $g.FillPath($bagBrush, $bagPath)
    }

    # 2. Bag Handle
    $hWidth = $bWidth * 0.44
    $hHeight = $emblemSize * 0.26
    $hx = $bx + ($bWidth - $hWidth) / 2
    $hy = $by - $hHeight * 0.65
    $hPen = if ($type -eq "monochrome") { New-Object System.Drawing.Pen($white, [Math]::Max(2, [int]($size * 0.038))) } else { New-Object System.Drawing.Pen($white, [Math]::Max(2, [int]($size * 0.038))) }
    $g.DrawArc($hPen, $hx, $hy, $hWidth, $hHeight * 2, 180, 180)

    # 3. Barcode Lines on the Bag
    $barTop = $by + $bHeight * 0.22
    $barH = $bHeight * 0.46
    $barColor = if ($type -eq "monochrome") { $indigoDark } else { $indigoPrimary }
    $barBrush = New-Object System.Drawing.SolidBrush($barColor)

    $colW = $bWidth * 0.05
    $startX = $bx + $bWidth * 0.16
    $barOffsets = @(0.0, 0.08, 0.15, 0.26, 0.33, 0.42, 0.52, 0.60, 0.68)
    $barThickness = @(1.2, 1.8, 1.0, 2.0, 1.2, 1.0, 1.8, 1.0, 1.4)
    for ($i = 0; $i -lt $barOffsets.Length; $i++) {
        $x = $startX + ($bWidth * $barOffsets[$i])
        $w = $colW * $barThickness[$i]
        $g.FillRectangle($barBrush, [float]$x, [float]$barTop, [float]$w, [float]$barH)
    }

    # 4. Red/Emerald Laser Scanner Beam cutting horizontally across barcode
    $laserY = $barTop + $barH * 0.5
    $g.DrawLine($laserPen, [float]($bx + $bWidth * 0.08), [float]$laserY, [float]($bx + $bWidth * 0.92), [float]$laserY)

    # 5. Little Currency/Card/Verified Dot at bottom corner
    $dotSize = $bWidth * 0.14
    $dotX = $bx + $bWidth * 0.72
    $dotY = $by + $bHeight * 0.72
    $g.FillEllipse($accentBrush, [float]$dotX, [float]$dotY, [float]$dotSize, [float]$dotSize)

    $dir = Split-Path -Parent $outputPath
    if (!(Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    $bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)

    $g.Dispose()
    $bmp.Dispose()
}

$baseRes = "kotlin_billing_app\app\src\main\res"

# Densities and sizes (launcher icon size, foreground size)
$densities = @{
    "mipmap-mdpi"    = @{ icon = 48;  fg = 108 }
    "mipmap-hdpi"    = @{ icon = 72;  fg = 162 }
    "mipmap-xhdpi"   = @{ icon = 96;  fg = 216 }
    "mipmap-xxhdpi"  = @{ icon = 144; fg = 324 }
    "mipmap-xxxhdpi" = @{ icon = 192; fg = 432 }
}

foreach ($density in $densities.Keys) {
    $sizes = $densities[$density]
    $iconSize = $sizes.icon
    $fgSize = $sizes.fg

    $targetDir = Join-Path $baseRes $density

    Write-Host "Generating for $density (Icon: ${iconSize}x${iconSize}, FG: ${fgSize}x${fgSize})..."
    Draw-DokandaarIcon -size $iconSize -type "square" -outputPath (Join-Path $targetDir "ic_launcher.png")
    Draw-DokandaarIcon -size $iconSize -type "round" -outputPath (Join-Path $targetDir "ic_launcher_round.png")
    Draw-DokandaarIcon -size $fgSize -type "foreground" -outputPath (Join-Path $targetDir "ic_launcher_foreground.png")
    Draw-DokandaarIcon -size $fgSize -type "monochrome" -outputPath (Join-Path $targetDir "ic_launcher_monochrome.png")
}

# Generate high-res splash logo in drawable
$drawableDir = Join-Path $baseRes "drawable"
Write-Host "Generating splash logo (512x512)..."
Draw-DokandaarIcon -size 512 -type "splash" -outputPath (Join-Path $drawableDir "dokandaar_splash_logo.png")

Write-Host "All icons and splash logo successfully generated!"
