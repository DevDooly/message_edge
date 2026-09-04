param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$midnight = [System.Drawing.Color]::FromArgb(255, 6, 11, 30)
$midnightRaised = [System.Drawing.Color]::FromArgb(255, 10, 23, 51)
$panelColor = [System.Drawing.Color]::FromArgb(255, 16, 39, 68)
$rowColor = [System.Drawing.Color]::FromArgb(255, 23, 56, 95)
$cloudDancer = [System.Drawing.Color]::FromArgb(255, 240, 238, 233)
$cyan = [System.Drawing.Color]::FromArgb(255, 0, 215, 215)
$blue = [System.Drawing.Color]::FromArgb(255, 67, 135, 255)
$violet = [System.Drawing.Color]::FromArgb(255, 124, 92, 252)

function New-RoundedRectanglePath {
    param(
        [float]$X,
        [float]$Y,
        [float]$Width,
        [float]$Height,
        [float]$Radius
    )

    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $diameter = $Radius * 2
    $path.AddArc($X, $Y, $diameter, $diameter, 180, 90)
    $path.AddArc($X + $Width - $diameter, $Y, $diameter, $diameter, 270, 90)
    $path.AddArc($X + $Width - $diameter, $Y + $Height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($X, $Y + $Height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function New-RailPath {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new(
        [System.Drawing.Drawing2D.FillMode]::Alternate
    )

    $path.StartFigure()
    $path.AddArc(81, 24, 5, 5, 180, 180)
    $path.AddLine(86, 26.5, 86, 81.5)
    $path.AddArc(81, 79, 5, 5, 0, 180)
    $path.AddLine(81, 81.5, 81, 60)
    $path.AddLine(81, 60, 76, 54)
    $path.AddLine(76, 54, 81, 48)
    $path.CloseFigure()

    $hole = [System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new(80.2, 50.7),
        [System.Drawing.PointF]::new(84.3, 54),
        [System.Drawing.PointF]::new(80.2, 57.3)
    )
    $path.AddPolygon($hole)
    return $path
}

function Set-HighQualityGraphics {
    param([System.Drawing.Graphics]$Graphics)

    $Graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $Graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $Graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $Graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
}

function Draw-Background {
    param([System.Drawing.Graphics]$Graphics)

    $bounds = [System.Drawing.RectangleF]::new(0, 0, 108, 108)
    $brush = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        $bounds,
        $midnight,
        $midnightRaised,
        45
    )
    try {
        $Graphics.FillRectangle($brush, $bounds)
    } finally {
        $brush.Dispose()
    }
}

function Draw-Foreground {
    param(
        [System.Drawing.Graphics]$Graphics,
        [switch]$Monochrome
    )

    if ($Monochrome) {
        $white = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
        $panelOutline = New-RoundedRectanglePath -X 23 -Y 24 -Width 52 -Height 60 -Radius 7
        $outlinePen = [System.Drawing.Pen]::new([System.Drawing.Color]::White, 2.5)
        $outlinePen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
        try {
            $Graphics.DrawPath($outlinePen, $panelOutline)
            foreach ($rowY in @(39, 54, 69)) {
                $Graphics.FillEllipse($white, 28, $rowY - 4, 8, 8)
                $bar = New-RoundedRectanglePath -X 38.25 -Y ($rowY - 2.75) -Width 30.75 -Height 5.5 -Radius 2.75
                try {
                    $Graphics.FillPath($white, $bar)
                } finally {
                    $bar.Dispose()
                }
            }
            $rail = New-RailPath
            try {
                $Graphics.FillPath($white, $rail)
            } finally {
                $rail.Dispose()
            }
        } finally {
            $outlinePen.Dispose()
            $panelOutline.Dispose()
            $white.Dispose()
        }
        return
    }

    $panelBrush = [System.Drawing.SolidBrush]::new($panelColor)
    $rowBrush = [System.Drawing.SolidBrush]::new($rowColor)
    $informationBrush = [System.Drawing.SolidBrush]::new($cloudDancer)
    try {
        $panel = New-RoundedRectanglePath -X 23 -Y 24 -Width 52 -Height 60 -Radius 7
        try {
            $Graphics.FillPath($panelBrush, $panel)
        } finally {
            $panel.Dispose()
        }

        foreach ($rowY in @(33, 48, 63)) {
            $row = New-RoundedRectanglePath -X 26 -Y $rowY -Width 46 -Height 12 -Radius 6
            try {
                $Graphics.FillPath($rowBrush, $row)
            } finally {
                $row.Dispose()
            }
        }

        foreach ($rowCenterY in @(39, 54, 69)) {
            $Graphics.FillEllipse($informationBrush, 28, $rowCenterY - 4, 8, 8)
            $bar = New-RoundedRectanglePath -X 38.25 -Y ($rowCenterY - 2.75) -Width 30.75 -Height 5.5 -Radius 2.75
            try {
                $Graphics.FillPath($informationBrush, $bar)
            } finally {
                $bar.Dispose()
            }
        }

        $rail = New-RailPath
        $railBounds = [System.Drawing.RectangleF]::new(76, 24, 10, 60)
        $railBrush = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
            $railBounds,
            $cyan,
            $violet,
            90
        )
        $blend = [System.Drawing.Drawing2D.ColorBlend]::new(3)
        $blend.Colors = [System.Drawing.Color[]]@($cyan, $blue, $violet)
        $blend.Positions = [single[]]@(0, 0.52, 1)
        $railBrush.InterpolationColors = $blend
        try {
            $Graphics.FillPath($railBrush, $rail)
        } finally {
            $railBrush.Dispose()
            $rail.Dispose()
        }
    } finally {
        $panelBrush.Dispose()
        $rowBrush.Dispose()
        $informationBrush.Dispose()
    }
}

function New-RenderedIcon {
    param(
        [int]$Size,
        [ValidateSet("AdaptiveBackground", "AdaptiveForeground", "FullSquare", "Squircle", "Circle", "Monochrome")]
        [string]$Mode
    )

    $bitmap = [System.Drawing.Bitmap]::new(
        $Size,
        $Size,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    Set-HighQualityGraphics -Graphics $graphics
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $scale = $Size / 108.0
    $graphics.ScaleTransform($scale, $scale)

    try {
        if ($Mode -eq "Squircle") {
            $clip = New-RoundedRectanglePath -X 0 -Y 0 -Width 108 -Height 108 -Radius 23
            $graphics.SetClip($clip)
            $clip.Dispose()
        } elseif ($Mode -eq "Circle") {
            $clip = [System.Drawing.Drawing2D.GraphicsPath]::new()
            $clip.AddEllipse(0, 0, 108, 108)
            $graphics.SetClip($clip)
            $clip.Dispose()
        }

        if ($Mode -in @("AdaptiveBackground", "FullSquare", "Squircle", "Circle", "Monochrome")) {
            Draw-Background -Graphics $graphics
        }
        if ($Mode -in @("AdaptiveForeground", "FullSquare", "Squircle", "Circle")) {
            Draw-Foreground -Graphics $graphics
        } elseif ($Mode -eq "Monochrome") {
            Draw-Foreground -Graphics $graphics -Monochrome
        }
    } finally {
        $graphics.Dispose()
    }

    return $bitmap
}

function Export-IconPng {
    param(
        [int]$Size,
        [string]$Mode,
        [string]$Path
    )

    $directory = Split-Path -Parent $Path
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    $bitmap = New-RenderedIcon -Size $Size -Mode $Mode
    try {
        $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }
}

$resourceRoot = Join-Path $RepositoryRoot "app\src\main\res"
$densityScale = [ordered]@{
    mdpi = 1.0
    hdpi = 1.5
    xhdpi = 2.0
    xxhdpi = 3.0
    xxxhdpi = 4.0
}

foreach ($density in $densityScale.GetEnumerator()) {
    $adaptiveSize = [int](108 * $density.Value)
    $legacySize = [int](48 * $density.Value)
    $drawableDirectory = Join-Path $resourceRoot "drawable-$($density.Key)"
    $mipmapDirectory = Join-Path $resourceRoot "mipmap-$($density.Key)"

    Export-IconPng -Size $adaptiveSize -Mode "AdaptiveBackground" -Path (Join-Path $drawableDirectory "ic_launcher_background.png")
    Export-IconPng -Size $adaptiveSize -Mode "AdaptiveForeground" -Path (Join-Path $drawableDirectory "ic_launcher_foreground.png")
    Export-IconPng -Size $legacySize -Mode "Squircle" -Path (Join-Path $mipmapDirectory "ic_launcher.png")
    Export-IconPng -Size $legacySize -Mode "Circle" -Path (Join-Path $mipmapDirectory "ic_launcher_round.png")
}

$iconRoot = Join-Path $RepositoryRoot "docs\icons"
Export-IconPng -Size 1024 -Mode "FullSquare" -Path (Join-Path $iconRoot "master\notification_edge_sliding_panel_1024.png")
Export-IconPng -Size 512 -Mode "FullSquare" -Path (Join-Path $iconRoot "play_store\notification_edge_sliding_panel_512.png")
Export-IconPng -Size 1024 -Mode "Squircle" -Path (Join-Path $iconRoot "preview\notification_edge_sliding_panel_preview.png")
Export-IconPng -Size 512 -Mode "Monochrome" -Path (Join-Path $iconRoot "preview\notification_edge_sliding_panel_monochrome.png")

$manifestPaths = @(
    "docs/icons/README_ICON_PACK.md",
    "docs/icons/design_tokens.json",
    "docs/icons/source/notification_edge_sliding_panel_concept.png",
    "docs/icons/master/notification_edge_sliding_panel_master.svg",
    "docs/icons/master/notification_edge_sliding_panel_monochrome.svg",
    "docs/icons/master/notification_edge_sliding_panel_1024.png",
    "docs/icons/play_store/notification_edge_sliding_panel_512.png",
    "docs/icons/preview/notification_edge_sliding_panel_preview.png",
    "docs/icons/preview/notification_edge_sliding_panel_monochrome.png",
    "scripts/generate-launcher-assets.ps1",
    "app/src/main/res/drawable/ic_launcher_background.xml",
    "app/src/main/res/drawable/ic_launcher_foreground.xml",
    "app/src/main/res/drawable/ic_launcher_monochrome.xml",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
    "app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml",
    "app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml"
)

foreach ($density in $densityScale.Keys) {
    $manifestPaths += "app/src/main/res/drawable-$density/ic_launcher_background.png"
    $manifestPaths += "app/src/main/res/drawable-$density/ic_launcher_foreground.png"
    $manifestPaths += "app/src/main/res/mipmap-$density/ic_launcher.png"
    $manifestPaths += "app/src/main/res/mipmap-$density/ic_launcher_round.png"
}

$manifest = foreach ($relativePath in $manifestPaths) {
    $item = Get-Item -LiteralPath (Join-Path $RepositoryRoot $relativePath)
    [ordered]@{
        path = $relativePath
        bytes = $item.Length
        sha256 = (Get-FileHash -LiteralPath $item.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    }
}

$manifestPath = Join-Path $iconRoot "FILE_MANIFEST.json"
$manifest | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath $manifestPath -Encoding utf8

Write-Output "슬라이딩 패널 런처 아이콘 자산 $($manifest.Count)개와 매니페스트 생성을 완료했습니다."
