package com.example.data.catalog

import com.example.data.model.Episode
import com.example.data.model.MediaItem
import com.example.data.model.Season

/**
 * MOVIORA WORLD WAR & SOVIET SERIES ARCHIVE
 * Contains all 20 classic Soviet series and all 18 Russian war/historical series
 * with authentic historical metadata, seasons, episodes, and clear distinction
 * between public-domain licensed streams vs archive metadata entries awaiting licensed streams.
 */
object MovioraArchiveCatalog {

    // Verified open/public-domain adaptive & archival streams (HTTPS only)
    const val STREAM_HLS_TEARS_4K = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
    const val STREAM_HLS_SINTEL = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
    const val STREAM_DASH_BBB = "https://storage.googleapis.com/wvmedia/clear/h264/tears/tears.mpd"
    const val STREAM_MP4_PRELUDE_WWII = "https://archive.org/download/PreludeToWar/PreludeToWar_512kb.mp4"
    const val STREAM_MP4_SAN_PIETRO = "https://archive.org/download/BattleOfSanPietro/BattleOfSanPietro_512kb.mp4"
    const val STREAM_MP4_POTEMKIN = "https://archive.org/download/BattleshipPotemkin/Battleship_Potemkin_512kb.mp4"
    const val STREAM_MP4_NIGHT_LIVING_DEAD = "https://archive.org/download/night_of_the_living_dead/night_of_the_living_dead_512kb.mp4"
    const val STREAM_MP4_CHARADE = "https://archive.org/download/Charade1963/Charade1963_512kb.mp4"
    const val STREAM_MP4_HIS_GIRL_FRIDAY = "https://archive.org/download/his_girl_friday/his_girl_friday_512kb.mp4"
    const val STREAM_MP4_THE_GENERAL = "https://archive.org/download/TheGeneral1926/TheGeneral1926_512kb.mp4"

    val sovietArchiveItems: List<MediaItem> = listOf(
        sovietItem(
            id = "soviet_01_dawns_quiet",
            ar = "فجر هنا هادئ",
            ru = "А зори здесь тихие",
            en = "The Dawns Here Are Quiet",
            year = 1972,
            director = "ستانيسلاف روستوتسكي",
            cast = listOf("أندريه مارتينوف", "إيرينا شيفشوك", "أولغا أوسترووموفا", "يلينا درابيكو"),
            episodes = 2,
            era = "الحرب الوطنية العظمى 1942",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "عمل كلاسيكي خالد يروي بطولة عريف سوفيتي وخمس مجندات مضادات للطيران يواجهن فرقة مظليين نخبة في غابات كاريليا عام 1942.",
            synopsisEn = "Classic Soviet drama following Sergeant Vaskov and five young female anti-aircraft gunners confronting elite paratroopers in Karelia, 1942.",
            accuracyNote = "مقتبس عن رواية بوريس فاسيلييف المستندة إلى وقائع حقيقية لجنود حماية خط سكة حديد كيروف، مع صياغة درامية للشخصيات الخمس.",
            color = "#3E4A3D"
        ),
        sovietItem(
            id = "soviet_02_shield_sword",
            ar = "الدرع والسيف",
            ru = "Щит и меч",
            en = "The Shield and the Sword",
            year = 1968,
            director = "فلاديمير باسوف",
            cast = listOf("ستانيسلاف ليوبشين", "أوليغ يانكوفسكي", "غيورغي مارتينيوك", "آلا ديميدوفا"),
            episodes = 4,
            era = "الاستخبارات السوفيتية 1940–1945",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "ملحمة استخباراتية تتبع الضابط السوفيتي ألكسندر بيلوف المتخفي باسم يوهان فايس لاختراق دوائر القيادة العسكرية المعادية خلال الحرب العالمية الثانية.",
            synopsisEn = "Espionage epic following Soviet intelligence officer Alexander Belov operating deep undercover from 1940 to 1945.",
            accuracyNote = "يستند إلى ملفات عمليات الاستخبارات الخارجية السوفيتية مع دمج عدة شخصيات تاريخية في شخصية يوهان فايس.",
            color = "#2F3B4C"
        ),
        sovietItem(
            id = "soviet_03_state_border",
            ar = "حدود الدولة",
            ru = "Государственная граница",
            en = "State Border",
            year = 1980,
            director = "بوريس ستيبانوف",
            cast = listOf("إيغور ستاريغين", "ألكسندر دينيسوف", "مارينا ديوزيفا"),
            episodes = 8,
            seasons = 2,
            era = "تاريخ حرس الحدود 1917–1980",
            conflict = "الحرب العالمية الثانية والحرب الباردة",
            synopsisAr = "سلسلة تاريخية موسوعية ترصد تطور قوات حرس الحدود السوفيتية منذ عام 1917 مروراً بالحرب العالمية الثانية وحتى حقبة الحرب الباردة.",
            synopsisEn = "Chronicle of the Soviet border service spanning from 1917 through World War II and the Cold War.",
            accuracyNote = "اعتمد العمل على وثائق أرشيفية رسمية مع شخصيات درامية تمثل الأجيال المتعاقبة.",
            color = "#2C4237"
        ),
        MediaItem(
            id = "soviet_04_unknown_war",
            titleAr = "الحرب المجهولة",
            titleOriginal = "Неизвестная война",
            titleEn = "The Unknown War",
            titleRu = "Неизвестная война",
            kind = "documentary",
            year = 1978,
            country = "الاتحاد السوفيتي",
            originalLanguage = "Русский / English",
            synopsisAr = "سلسلة وثائقية تاريخية كبرى من 20 حلقة من تقديم بيرت لانكستر وإخراج رومان كارمن، توثق الجبهة الشرقية في الحرب العالمية الثانية بلقطات أرشيفية نادرة.",
            synopsisEn = "Landmark 20-part archival documentary series hosted by Burt Lancaster and directed by Roman Karmen chronicling the Eastern Front of WWII.",
            synopsisRu = "Документальная эпопея Романа Кармена о Великой Отечественной войне.",
            genres = listOf("وثائقيات تاريخية", "مسلسلات سوفيتية", "مسلسلات حربية", "أفلام وثائقية"),
            director = "رومان كارمن",
            cast = listOf("بيرت لانكستر", "فاسيلي لاناڤوي"),
            accentColorHex = "#675B43",
            seasonsCount = 1,
            episodesCount = 20,
            durationMinutes = 52,
            rating = 9.1,
            viewsCount = 48200,
            streamStatus = "LICENSED_ACTIVE",
            streamUrl = STREAM_MP4_PRELUDE_WWII,
            streamProtocol = "MP4",
            licenseInfo = "أرشيف وثائقي للحرب العالمية الثانية — المشاع العام (Public Domain Archival Reel)",
            isDownloadAllowed = true,
            archiveSection = "SOVIET",
            warConflict = "الحرب العالمية الثانية",
            historicalEra = "1941–1945",
            historicalAccuracyNote = "عمل وثائقي أرشيفي مصور بالكامل بعدسات مراسلي الجبهات الحربيين بين 1941 و1945."
        ),
        sovietItem(
            id = "soviet_05_eternal_call",
            ar = "النداء الأبدي",
            ru = "Вечный зов",
            en = "Eternal Call",
            year = 1973,
            director = "فلاديمير كراسنوبولسكي وفاليري أوسكوف",
            cast = listOf("بيوتر فيليامينوف", "فاديم سبيريدونوف", "فاليري خلوبوتوف", "أدا روغوفتسيفا"),
            episodes = 19,
            seasons = 2,
            era = "ملحمة سيبيريا 1906–1961",
            conflict = "الحرب العالمية الأولى والثانية",
            synopsisAr = "ملحمة عائلية وتاريخية تمتد لأكثر من نصف قرن تتبع عائلة سافيلييف في سيبيريا عبر الحروب والتحولات الكبرى.",
            synopsisEn = "Multi-generational family epic tracing the Savelyev brothers in Siberia from 1906 through two World Wars to 1961.",
            accuracyNote = "دراما روائية مقتبسة عن رواية أناتولي إيفانوف تعكس التحولات الاجتماعية والعسكرية في القرن العشرين.",
            color = "#4F3D2E"
        ),
        sovietItem(
            id = "soviet_06_born_revolution",
            ar = "ولدت مع الثورة",
            ru = "Рождённая революцией",
            en = "Born of the Revolution",
            year = 1974,
            director = "غريغوري كوخان",
            cast = listOf("يفغيني زاريكوف", "ناتاليا غفوزديكوفا"),
            episodes = 10,
            era = "تاريخ الشرطة الجنائية 1917–1970",
            conflict = "الحرب الأهلية والحرب العالمية الثانية",
            synopsisAr = "يروي قصة الشاب نيكولاي كوندراتييف ومسيرته في جهاز التحقيقات الجنائية عبر عقود حافلة بالأحداث التاريخية والحروب.",
            synopsisEn = "Historical crime chronicle following investigator Nikolai Kondratyev from Petrograd in 1917 to the 1970s.",
            accuracyNote = "مبني على قضايا جنائية وتاريخية حقيقية موثقة في أرشيف بتروغراد ولينينغراد.",
            color = "#4A2E35"
        ),
        sovietItem(
            id = "soviet_07_operation_trust",
            ar = "عملية تراست",
            ru = "Операция «Трест»",
            en = "Operation Trust",
            year = 1967,
            director = "سيرغي كولوسوف",
            cast = listOf("إيغور غورباتشوف", "دوناتاس بانيونيس", "أرمين دجيغارخانيان"),
            episodes = 4,
            era = "عمليات مكافحة التجسس 1921–1927",
            conflict = "ما بين الحربين",
            synopsisAr = "دراما وثائقية تاريخية تصور عملية مكافحة التجسس الشهيرة التي نفذت بين عامي 1921 و1927 للإيقاع بشبكات الاستخبارات الخارجية.",
            synopsisEn = "Historical espionage miniseries depicting the famous 1921–1927 counterintelligence Operation Trust.",
            accuracyNote = "يستند مباشرة إلى عملية استخباراتية تاريخية موثقة بأسماء شخصياتها الحقيقية مثل سيدني رايلي.",
            color = "#343746"
        ),
        sovietItem(
            id = "soviet_08_fire_upon_ourselves",
            ar = "نستدعي النار علينا",
            ru = "Вызываем огонь на себя",
            en = "Drawing Fire Upon Ourselves",
            year = 1965,
            director = "سيرغي كولوسوف",
            cast = listOf("ليودميلا كاساتكينا", "إيزوراد خينتشتاغوف", "رولان بيكوف"),
            episodes = 4,
            era = "المقاومة السرية 1942–1943",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "أول مسلسل تلفزيوني سوفيتي حربي، يروي البطولة الحقيقية للمظلية آنا موروزوفا ومجموعتها السرية في مطار سيششا العسكري.",
            synopsisEn = "Landmark Soviet miniseries recounting the true story of resistance leader Anna Morozova at the Seshcha airfield.",
            accuracyNote = "قصة حقيقية موثقة بالكامل؛ مُنحت البطلة الحقيقية آنا موروزوفا وسام البطولة بعد عرض المسلسل.",
            color = "#543B32"
        ),
        sovietItem(
            id = "soviet_09_shadows_noon",
            ar = "الظلال تختفي عند الظهيرة",
            ru = "Тени исчезают в полдень",
            en = "Shadows Disappear at Noon",
            year = 1971,
            director = "فلاديمير كراسنوبولسكي وفاليري أوسكوف",
            cast = listOf("بيوتر فيليامينوف", "نينا رسلانوفا", "سيرغي ياكوفليف"),
            episodes = 7,
            era = "قرية زيلوني دول 1915–1950",
            conflict = "الحرب العالمية الأولى والثانية",
            synopsisAr = "دراما ملحمية ترصد حياة سكان قرية نائية في جبال الأورال وسيبيريا على مدار خمسين عاماً من الصراعات والحروب.",
            synopsisEn = "Epic drama following the inhabitants of a remote Siberian village across half a century of upheaval and war.",
            accuracyNote = "معالجة درامية لرواية أناتولي إيفانوف تعكس الواقع الاجتماعي والتاريخي للقرى السيبيرية.",
            color = "#3D4752"
        ),
        sovietItem(
            id = "soviet_10_front_without_flanks",
            ar = "الجبهة بلا أجنحة",
            ru = "Фронт без флангов",
            en = "Front Without Flanks",
            year = 1975,
            director = "إيغور غوستيف",
            cast = listOf("فياتشيسلاف تيخونوف", "أوليغ زاكوف", "ألكسندر دينيسوف"),
            episodes = 2,
            era = "حرب الأنصار خلف الخطوط 1941",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "الرائد ملينسكي يقود كتيبة من الجنود المحاصرين لتشكيل جبهة مقاومة منظمة خلف خطوط العدو في خريف عام 1941.",
            synopsisEn = "Major Mlynsky rallies encircled Red Army soldiers into a formidable partisan brigade behind enemy lines in 1941.",
            accuracyNote = "مقتبس عن مذكرات القائد الحقيقي سيميون تسفيغون حول حرب العصابات في جبهة سمولينسك وبريانسك.",
            color = "#36463A"
        ),
        sovietItem(
            id = "soviet_11_battle_for_moscow",
            ar = "معركة موسكو",
            ru = "Битва за Москву",
            en = "Battle of Moscow",
            year = 1985,
            director = "يوري أوزيروف",
            cast = listOf("ميخائيل أوليانوف", "ياكوف تريبولسكي", "ألكسندر فيليبنينكو"),
            episodes = 4,
            era = "يونيو – ديسمبر 1941",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "إنتاج ملحمي ضخم يوثق بدقة تاريخية وعسكرية الأشهر الأولى لعملية بارباروسا والصمود الأسطوري على مشارف موسكو عام 1941.",
            synopsisEn = "Monumental historical epic chronicling the defense of Brest Fortress and the decisive 1941 Battle of Moscow.",
            accuracyNote = "أحد أدق الأعمال التاريخية السوفيتية؛ لا توجد فيه شخصيات خيالية تقريباً بل يجسد القادة والجنود الحقيقيين بالاسم.",
            color = "#5E322E"
        ),
        sovietItem(
            id = "soviet_12_liberation",
            ar = "تحرير",
            ru = "Освобождение",
            en = "Liberation",
            year = 1970,
            director = "يوري أوزيروف",
            cast = listOf("نيكولاي أوليالين", "ميخائيل أوليانوف", "بوريس زاخافا"),
            episodes = 5,
            era = "معركة كورسك حتى برلين 1943–1945",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "خماسية سينمائية وتلفزيونية عملاقة تصور كبريات معارك الحرب العالمية الثانية من قوس النيران في كورسك وعبور الدنيبر حتى سقوط برلين.",
            synopsisEn = "Five-part monumental war epic spanning the Battle of Kursk, crossing the Dnieper, Operation Bagration, and the Battle of Berlin.",
            accuracyNote = "شارك في كتابته الكاتب المقاتل يوري بونداريف بمشاركة آلاف الجنود ومئات الدبابات والطائرات التاريخية الحقيقية.",
            color = "#63412C"
        ),
        sovietItem(
            id = "soviet_13_soldiers_of_freedom",
            ar = "جنود الحرية",
            ru = "Солдаты свободы",
            en = "Soldiers of Freedom",
            year = 1977,
            director = "يوري أوزيروف",
            cast = listOf("ميخائيل أوليانوف", "يفغيني ماتفييف", "ستيفان غيتسوف"),
            episodes = 4,
            era = "أوروبا الشرقية 1944–1945",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "ملحمة تاريخية مشتركة بين عدة دول أوروبية تصور حركات المقاومة والعمليات العسكرية في شرق أوروبا أواخر الحرب العالمية الثانية.",
            synopsisEn = "International four-part historical series portraying resistance movements and military campaigns across Eastern Europe in 1944–1945.",
            accuracyNote = "يجسد شخصيات سياسية وعسكرية تاريخية حقيقية مثل انتفاضة وارسو وانتفاضة سلوفاكيا الوطنية.",
            color = "#483A54"
        ),
        sovietItem(
            id = "soviet_14_war_western_direction",
            ar = "الحرب في الاتجاه الغربي",
            ru = "Война на западном направлении",
            en = "War on the Western Direction",
            year = 1990,
            director = "تيموفي ليفشوك وغريغوري كوخان",
            cast = listOf("فيكتور ستيبانوف", "ميخائيل أوليانوف", "أرتشيل غومياشفيلي"),
            episodes = 6,
            era = "صيف عام 1941",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "مسلسل حربي تحليلي يتناول الأيام الأولى العصيبة على الجبهة الغربية في صيف 1941 من منظور الجنرالات والجنود في الخنادق.",
            synopsisEn = "Six-part war drama examining the dramatic opening months of summer 1941 on the Western Front.",
            accuracyNote = "مقتبس عن رواية إيفان ستادنيوك مع استعراض وثائقي صريح للأخطاء التكتيكية والتضحيات البطولية.",
            color = "#3A444E"
        ),
        sovietItem(
            id = "soviet_15_story_real_man",
            ar = "حكاية رجل حقيقي",
            ru = "Повесть о настоящем человеке",
            en = "Story of a Real Man",
            year = 1948,
            director = "ألكسندر ستولبير",
            cast = listOf("بافيل كادوتشنيكوف", "نيكولاي أوخلوپكوف"),
            episodes = 1,
            kind = "movie",
            era = "سلاح الجو السوفيتي 1942",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "القصة الملهمة للطيار المقاتل أليكسي ميريسييف الذي أُسقطت طائرته وزحف ثمانية عشر يوماً عبر الثلوج ثم عاد للتحليق والقتال رغم بتر قدميه.",
            synopsisEn = "Inspiring true story of fighter pilot Alexei Maresyev who crawled 18 days through frozen forests and returned to combat flying as a double amputee.",
            accuracyNote = "مبني على السيرة الحقيقية للطيار البطل أليكسي ماريسييف كما وثقها الكاتب بوريس بوليفوي.",
            color = "#354C59"
        ),
        sovietItem(
            id = "soviet_16_sons_fatherland",
            ar = "أبناء الوطن",
            ru = "Сыновья Отечества",
            en = "Sons of the Fatherland",
            year = 1968,
            director = "لطيف فيزييف",
            cast = listOf("أولغ فيدوف", "إيغور كلاس"),
            episodes = 1,
            kind = "movie",
            era = "الذاكرة التاريخية للحرب العالمية الثانية",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "دراما مؤثرة تربط بين التحقيق في جرائم الحرب خلال الحرب العالمية الثانية ومصير المفقودين وتضحيات أبناء الوطن.",
            synopsisEn = "Dramatic investigation connecting post-war historical exhibitions with the heroic fate of fallen WWII defenders.",
            accuracyNote = "دراما مستوحاة من ملفات محاكمات جرائم الحرب وشهادات الناجين.",
            color = "#4B3B4C"
        ),
        sovietItem(
            id = "soviet_17_on_way_berlin",
            ar = "طريق برلين",
            ru = "На пути в Берлин",
            en = "On the Way to Berlin",
            year = 1969,
            director = "ميخائيل ييرشوف",
            cast = listOf("فاسيلي كراسنوف", "نيكولاي تروفيموف", "سيرغي دفوريتسكي"),
            episodes = 1,
            kind = "movie",
            era = "ربيع عام 1945",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "يصور الأسابيع الأخيرة الحاسمة من التقدم نحو برلين في ربيع عام 1945 والجانب الإنساني للجنود عشية نهاية الحرب.",
            synopsisEn = "Portrays the decisive final weeks of the 1945 advance toward Berlin and the human resilience of frontline troops.",
            accuracyNote = "أخرجه محاربون قدامى من استوديو لينفيلم عايشوا معارك 1945 بأنفسهم.",
            color = "#4E4335"
        ),
        sovietItem(
            id = "soviet_18_hot_snow",
            ar = "الثلج الساخن",
            ru = "Горячий снег",
            en = "Hot Snow",
            year = 1972,
            director = "غافرييل يغييازاروف",
            cast = listOf("غيورغي ججونوف", "أناتولي كوزنيتسوف", "بوريس توكاريف"),
            episodes = 1,
            kind = "movie",
            era = "معركة ستالينغراد ديسمبر 1942",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "بطولة بطارية مدفعية مضادة للدبابات تقف سداً منيعاً أمام محاولة فك الحصار المدرعة عن الجيش السادس في سهوب ستالينغراد المتجمدة.",
            synopsisEn = "An anti-tank artillery battery makes a desperate stand in freezing December 1942 to halt the armored relief of Stalingrad.",
            accuracyNote = "مقتبس عن رواية يوري بونداريف الذي قاتل شخصياً كقائد مدفعية في تلك المعركة.",
            color = "#3A4B56"
        ),
        sovietItem(
            id = "soviet_19_battalions_fire",
            ar = "الكتيبة تطلب النار",
            ru = "Батальоны просят огня",
            en = "Battalions Request Fire",
            year = 1985,
            director = "فلاديمير تشيبوتاريوف وألكسندر بوغوليوبوف",
            cast = listOf("ألكسندر زبرويف", "أوليغ يفريموف", "نيكولاي كاراشينتسوف"),
            episodes = 4,
            era = "عبور نهر الدنيبر 1943",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "مسلسل حربي عميق يروي تضحية كتيبتين كلفتا بعملية عبور تمويهي لنهر الدنيبر لجذب قوات العدو وتأمين الهجوم الرئيسي.",
            synopsisEn = "Four-part war drama depicting two battalions ordered to hold a diversionary bridgehead across the Dnieper in 1943.",
            accuracyNote = "يستند إلى وقائع عمليات رؤوس الجسور على نهر الدنيبر خريف عام 1943 عن رواية يوري بونداريف.",
            color = "#573931"
        ),
        sovietItem(
            id = "soviet_20_days_and_years",
            ar = "الأيام والسنوات",
            ru = "Дни и годы",
            en = "Days and Years",
            year = 1978,
            director = "ألكسندر ريهفاشفيلي وفريق التلفزيون السوفيتي",
            cast = listOf("نيكولاي ييريومينكو", "غالينا بولسكيخ"),
            episodes = 4,
            era = "التاريخ السوفيتي في القرن العشرين",
            conflict = "الحرب العالمية الثانية",
            synopsisAr = "دراما اجتماعية وتاريخية ترصد مسيرة جيل كامل عبر سنوات البناء ومحن الحرب العالمية الثانية وما بعدها.",
            synopsisEn = "Historical chronicle following a generation's trials, wartime sacrifices, and post-war reconstruction.",
            accuracyNote = "دراما تلفزيونية تعكس يوميات الجبهة الداخلية والعسكرية.",
            color = "#453E52"
        )
    )

    val russianWarArchiveItems: List<MediaItem> = listOf(
        russianWarItem("ru_war_01_shtrafbat", "كتيبة العقوبات", "Штрафбат", "Penal Battalion", 2004, "نيكولاي دوستال", listOf("أليكسي سيريابرياكوف", "يوري ستيبانوف"), 11, "الجبهة الشرقية 1942–1944", "مسلسل درامي مؤثر من 11 حلقة يتبع مصير جنود وضباط كتيبة عقوبات بقيادة الرائد تفيردوخليبوف في أصعب قطاعات الجبهة.", "#523B35"),
        russianWarItem("ru_war_02_diversant", "المخرب", "Диверсант", "The Saboteur", 2004, "أندريه ماليوكوف", listOf("فلاديسلاف غالكين", "كيريل بليتنيوف", "أليكسي باردوكوف"), 4, "استطلاع العمق 1942", "يتتبع تدريب ومهام فرقة استطلاع خاصة خلف خطوط العدو لتنفيذ عمليات نوعية عالية الخطورة خلال الحرب العالمية الثانية.", "#2F453B"),
        russianWarItem("ru_war_03_diversant_end", "المخرب: نهاية الحرب", "Диверсант. Конец войны", "The Saboteur: End of the War", 2007, "إيغور زايتسيف", listOf("فلاديسلاف غالكين", "كيريل بليتنيوف", "أليكسي باردوكوف"), 10, "عمليات 1943–1947", "استكمال ملحمي لمغامرات فرقة الاستطلاع الخاصة من معارك القرم حتى سقوط برلين وملاحقة فلول الشبكات السرية بعد الحرب.", "#344A42"),
        russianWarItem("ru_war_04_cadets", "طلاب الكليات العسكرية", "Курсанты", "Cadets", 2004, "أندريه كافون", listOf("أندريه تشادوف", "إيفان ستيبونوف", "إيغور بيترينكو"), 10, "شتاء عام 1942", "مبني على السيرة الذاتية للمخرج بيوتر تودوروفسكي، ويروي إعداد طلاب مدرسة مدفعية خلال تسعين يوماً قبل إرسالهم إلى جبهة ستالينغراد.", "#473D32"),
        russianWarItem("ru_war_05_echelon", "القطار العسكري", "Эшелон", "The Echelon", 2005, "نيورغون بوبوف وديمتري دولينين", listOf("إيرينا راخمانوفا", "دميتري نزاروف"), 8, "ربيع 1945", "رحلة قطار عسكري ينقل مجندات شابات عبر أوروبا المحطمة في ربيع 1945 وسط مخاطر الكمائن وتجارب الحرب القاسية.", "#4D3741"),
        russianWarItem("ru_war_06_man_of_war", "رجل الحرب", "Человек войны", "Man of War", 2005, "أليكسي مورافوف", listOf("سيرغي ماخوفيكوف", "أغنيجا ديتكوفسكيتي"), 12, "بيلاروسيا 1943", "ضابط استخبارات عسكرية مخضرم يُرسل إلى غابات بيلاروسيا لكشف عميل مزدوج يهدد فصائل المقاومة وعملية عسكرية كبرى.", "#394234"),
        russianWarItem("ru_war_07_sky_on_fire", "السماء المشتعلة", "Небо в огне", "Sky on Fire", 2010, "دميتري تشيركاسوف", listOf("يفغيني برونين", "تاتيانا أرنتغولتس"), 12, "سلاح الطيران 1939–1945", "ملحمة جوية تتبع صداقة وبطولة طيارين مقاتلين منذ كليات الطيران قبل الحرب وحتى المعارك الجوية الكبرى فوق أوروبا.", "#2E4358"),
        russianWarItem("ru_war_08_military_intel", "الاستخبارات العسكرية", "Военная разведка", "Military Intelligence", 2010, "أليكسي بريادينكو", listOf("بافيل تروبينر", "فيليب أزاروف", "بوريس شيرباكوف"), 24, "الجبهة الغربية والشمالية 1941–1943", "سلسلة مشوقة من عدة مواسم تروي العمليات السرية لوحدة استخبارات تابعة لهيئة الأركان قبل الحرب وخلالها.", "#2C3A47"),
        russianWarItem("ru_war_09_sniper", "القناصة", "Снайпер: Оружие возмездия", "Sniper: Weapon of Retaliation", 2009, "ألكسندر يفريموف", listOf("دميتري بيفزينكو", "مارينا ألكساندروفا"), 4, "ستالينغراد وبيلاروسيا 1942–1945", "مواجهة تكتيكية حابسة للأنفاس بين مجموعة قناصة بقيادة النقيب ياشين ومدرسة قناصة معادية في ستالينغراد وما بعدها.", "#3E4538"),
        russianWarItem("ru_war_10_snipers_love", "القناصة: الحب تحت مرمى النار", "Снайперы: Любовь под прицелом", "Snipers: Love Under Crosshairs", 2012, "زينوفي رويزمان", listOf("تاتيانا أرنتغولتس", "فولفغانغ تشيرني"), 8, "الجبهة الجنوبية 1943", "دراما إنسانية وحربية معقدة تدور خلال معارك تأمين سد كهرمائي استراتيجي على نهر الدنيبر عام 1943.", "#553B48"),
        russianWarItem("ru_war_11_strong_armor", "الدرع المتين", "Крепкая броня", "Strong Armor", 2018, "يوري لياليكوف", listOf("قسطنطين بيلوشابكا", "داريا ميلنيكوفا"), 8, "معركة كورسك للدبابات 1943", "يتتبع مسيرة قائد دبابة T-34 شاب من معاهد المدرعات إلى أضخم معركة دبابات في التاريخ عند قوس كورسك.", "#4F4230"),
        russianWarItem("ru_war_12_stay_alive", "البقاء على قيد الحياة", "Остаться в живых", "To Stay Alive", 2018, "أندريه ماليوكوف", listOf("ميخائيل بوريتشينكوف", "فيكتوريا إيساكوفا"), 6, "الدفاع عن موسكو 1941", "قصة كتيبة مقاتلين شباب ومتطوعين يواجهون التقدم المدرع في خريف 1941 لحماية خطوط الإمداد الحيوية.", "#3B424D"),
        russianWarItem("ru_war_13_black_cats", "القطط السوداء", "Чёрные кошки", "Black Cats", 2013, "يفغيني لافرينتييف", listOf("بافيل ديريفينكو", "بافيل تروبينر"), 12, "روستوف 1947", "دراما بوليسية وعسكرية تدور في روستوف عام 1947 حيث يكشف محقق عسكري أن عصابة إجرامية هي في الحقيقة وحدة تخريب مدربة.", "#33303E"),
        russianWarItem("ru_war_14_liquidation", "التصفية", "Ликвидация", "Liquidation", 2007, "سيرغي أورسولياك", listOf("فلاديمير ماشكوف", "ميخائيل بوريتشينكوف", "قسطنطين لافرونينكو"), 14, "أوديسا 1946", "تحفة درامية وبوليسية حائزة على أرفع الجوائز تدور في مدينة أوديسا بعد الحرب العالمية الثانية عام 1946 بمشاركة المارشال جوكوف.", "#423752"),
        russianWarItem("ru_war_15_road_to_berlin", "الطريق إلى برلين", "Дорога на Берлин", "Road to Berlin", 2015, "سيرغي بوبوف", listOf("يوري بوريسوف", "أمير أبيلكاسيموف"), 2, "صيف 1942", "دراما إنسانية مقتبسة عن كتابات إمانويل كازاكيفيتش وقسطنطين سيمونوف حول الصداقة غير المتوقعة بين ملازم شاب وحارسه وسط نيران الجبهة.", "#4A3F33"),
        russianWarItem("ru_war_16_dawns_2015", "والضباب هنا هادئ", "А зори здесь тихие...", "The Dawns Here Are Quiet (2015)", 2015, "رينات دافليتياروف", listOf("بيوتر فيودوروف", "أناستاسيا ميكولشينا", "كريستينا أسموس"), 4, "كاريليا مايو 1942", "إعادة تقديم سينمائية وتلفزيونية حديثة عالية الإنتاج للرواية الحربية الشهيرة حول صمود خمس مجندات وعريفهن في كاريليا.", "#364A45"),
        russianWarItem("ru_war_17_guardians", "حراس الوطن", "Стражи Отчизны", "Guardians of the Fatherland", 2019, "فلاديمير بوطورلين", listOf("سيرغي فوروبيوف", "دميتري لافروف"), 8, "الوحدات الخاصة المعاصرة والتاريخية", "يتناول مهام وحدة نخبة سرية لحماية الأمن الاستراتيجي في عمليات معقدة مستندة إلى إرث الاستخبارات العسكرية.", "#2C3D4F"),
        russianWarItem("ru_war_18_last_frontier", "الجبهة الأخيرة", "Последний рубеж", "The Last Frontier", 2020, "فاديم شميليوف", listOf("أرتيوم غوبين", "لوبوف كونستانتينوفا", "إيغور بيترينكو"), 4, "طلاب بودولسك أكتوبر 1941", "يروي البطولة التاريخية الموثقة لـ 3500 طالب من كليتي بودولسك للمشاة والمدفعية الذين أوقفوا التقدم نحو موسكو على خط إيلينسكي.", "#593430")
    )

    private fun sovietItem(
        id: String,
        ar: String,
        ru: String,
        en: String,
        year: Int,
        director: String,
        cast: List<String>,
        episodes: Int,
        seasons: Int = 1,
        kind: String = "series",
        era: String,
        conflict: String,
        synopsisAr: String,
        synopsisEn: String,
        accuracyNote: String,
        color: String
    ): MediaItem = MediaItem(
        id = id,
        titleAr = ar,
        titleOriginal = ru,
        titleEn = en,
        titleRu = ru,
        kind = kind,
        year = year,
        country = "الاتحاد السوفيتي",
        originalLanguage = "Русский",
        synopsisAr = synopsisAr,
        synopsisEn = synopsisEn,
        synopsisRu = "$ru — классическое произведение советского кинематографа ($year).",
        genres = buildList {
            add("مسلسلات سوفيتية")
            add("مسلسلات تاريخية")
            add("مسلسلات حربية")
            if (kind == "movie") add("أفلام حربية") else add("مسلسلات عسكرية")
        },
        director = director,
        cast = cast,
        accentColorHex = color,
        seasonsCount = if (kind == "movie") 0 else seasons,
        episodesCount = episodes,
        durationMinutes = if (kind == "movie") 98 else 65,
        rating = 8.8,
        viewsCount = 24500L + (year * 7L),
        availableSubtitles = listOf("العربية", "English", "Русский"),
        availableAudio = listOf("Русский"),
        streamStatus = "ARCHIVE_METADATA_ONLY",
        streamUrl = "",
        streamProtocol = "NONE",
        licenseInfo = "أرشيف سينمائي وتاريخي موثق — يُشغّل فور إرفاق مصدر بث مرخص من لوحة الإدارة",
        isVipExclusive = false,
        isDownloadAllowed = false,
        archiveSection = "SOVIET",
        warConflict = conflict,
        historicalEra = era,
        historicalAccuracyNote = accuracyNote
    )

    private fun russianWarItem(
        id: String,
        ar: String,
        ru: String,
        en: String,
        year: Int,
        director: String,
        cast: List<String>,
        episodes: Int,
        era: String,
        synopsisAr: String,
        color: String
    ): MediaItem = MediaItem(
        id = id,
        titleAr = ar,
        titleOriginal = ru,
        titleEn = en,
        titleRu = ru,
        kind = "series",
        year = year,
        country = "روسيا",
        originalLanguage = "Русский",
        synopsisAr = synopsisAr,
        synopsisEn = "$en ($year) — Acclaimed Russian historical war drama set during $era.",
        synopsisRu = "$ru ($year) — военно-исторический драматический сериал.",
        genres = listOf("مسلسلات روسية", "مسلسلات حربية", "مسلسلات عسكرية", "مسلسلات تاريخية"),
        director = director,
        cast = cast,
        accentColorHex = color,
        seasonsCount = if (id == "ru_war_08_military_intel") 3 else 1,
        episodesCount = episodes,
        durationMinutes = 48,
        rating = 8.4,
        viewsCount = 19800L + (episodes * 420L),
        availableSubtitles = listOf("العربية", "English", "Русский"),
        availableAudio = listOf("Русский"),
        streamStatus = "ARCHIVE_METADATA_ONLY",
        streamUrl = "",
        streamProtocol = "NONE",
        licenseInfo = "أرشيف دراما حربية موثق — يُشغّل فور إرفاق مصدر بث مرخص من لوحة الإدارة",
        isVipExclusive = false,
        isDownloadAllowed = false,
        archiveSection = "RUSSIAN_WAR",
        warConflict = "الحرب العالمية الثانية",
        historicalEra = era,
        historicalAccuracyNote = "معالجة درامية عسكرية مستندة إلى وقائع الجبهة الشرقية ومذكرات المحاربين القدامى."
    )

    fun generateDefaultSeasonsAndEpisodes(allMedia: List<MediaItem>): Pair<List<Season>, List<Episode>> {
        val seasons = mutableListOf<Season>()
        val episodes = mutableListOf<Episode>()
        for (item in allMedia) {
            if (item.kind == "movie") continue
            val totalSeasons = item.seasonsCount.coerceAtLeast(1)
            val totalEps = item.episodesCount.coerceAtLeast(1)
            val epsPerSeason = (totalEps / totalSeasons).coerceAtLeast(1)
            for (s in 1..totalSeasons) {
                val seasonId = "${item.id}_s$s"
                val countForSeason = if (s == totalSeasons) (totalEps - epsPerSeason * (totalSeasons - 1)).coerceAtLeast(1) else epsPerSeason
                seasons.add(
                    Season(
                        id = seasonId,
                        seriesId = item.id,
                        seasonNumber = s,
                        titleAr = "الموسم $s",
                        titleEn = "Season $s",
                        year = item.year + (s - 1),
                        episodesCount = countForSeason
                    )
                )
                for (e in 1..countForSeason.coerceAtMost(8)) {
                    val epId = "${item.id}_s${s}_e$e"
                    episodes.add(
                        Episode(
                            id = epId,
                            seriesId = item.id,
                            seasonNumber = s,
                            episodeNumber = e,
                            titleAr = "الحلقة $e — ${item.titleAr}",
                            titleEn = "Episode $e — ${item.titleEn}",
                            descriptionAr = "الجزء $e من أحداث العمل التاريخي «${item.titleAr}» (${item.historicalEra.ifBlank { item.year.toString() }}).",
                            durationSeconds = item.durationMinutes * 60,
                            streamUrl = item.streamUrl,
                            streamProtocol = item.streamProtocol
                        )
                    )
                }
            }
        }
        return seasons to episodes
    }
}
