# Phase 1: Developer Experience Observations

## Setup & Build

### What Worked Well
- **Mise integration**: `.mise.toml` provides clean task definitions (`mise run build`, `mise run install`)
- **Java version management**: Mise handles Java versions seamlessly
- **Standard Android structure**: Familiar Gradle + Android project layout

### Friction Points

#### 1. **Java Version Incompatibility**
- **Issue**: Java 25 (latest) caused Gradle 8.2 to fail with cryptic error: `Unsupported class file major version 69`
- **Solution**: Had to downgrade to Java 17
- **DX Impact**: Not obvious which Java version is compatible. Error message doesn't suggest version mismatch.
- **Time Lost**: ~5 minutes debugging

#### 2. **Gradle Configuration Conflicts**
- **Issue**: `settings.gradle` with `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` conflicts with `build.gradle` repositories
- **Solution**: Remove `allprojects {}` block from root build.gradle
- **DX Impact**: Modern Gradle practices conflict with older patterns. Not well documented.
- **Time Lost**: ~3 minutes

#### 3. **Signing Configuration**
- **Issue**: Build failed at `:app:validateSigningDebug` with `ExceptionInInitializerError` (no helpful message)
- **Solution**: Manually create debug keystore and add `signingConfigs` to build.gradle
- **DX Impact**: **Critical**: Should auto-generate debug keystore like standard Android Studio projects do
- **Time Lost**: ~10 minutes
- **Severity**: HIGH - This is basic functionality that should "just work"

#### 4. **Gradle Wrapper Bootstrap**
- **Issue**: No gradlew in fresh project, Gradle CLI not globally installed
- **Solution**: Manually download gradle-wrapper.jar, properties, and scripts
- **DX Impact**: Manual bootstrapping is tedious. Android Studio handles this automatically.
- **Time Lost**: ~5 minutes

#### 5. **Verbose Warnings**
- **Issue**: Multiple warnings about restricted methods, plugin versions
- **DX Impact**: Noise that obscures actual issues
- **Severity**: LOW but annoying

### Total Setup Time
- **Expected**: ~2 minutes (if everything worked)
- **Actual**: ~25 minutes (debugging issues)
- **Time Overhead**: 12.5x longer than expected

---

## Code Quality

### Positives
- **Simple API**: Canvas drawing is intuitive
- **Clear lifecycle**: `onDraw()` is easy to understand
- **Good defaults**: Paint antialiasing, standard shapes
- **Fast iteration**: Code changes → rebuild → see results

### API Observations
- **Documentation**: Canvas API is well documented
- **Discoverability**: Easy to find methods (drawCircle, drawRect, etc.)
- **Type safety**: Strong typing helps prevent errors
- **Examples**: Plenty of examples online

---

## Missing Developer Tools

### What Would Improve DX

1. **Auto-generated debug keystore**: Like Android Studio does automatically
2. **Better error messages**:
   - "Java version X not supported, use Java Y" instead of class file version errors
   - Signing errors should suggest keystore generation
3. **Gradle wrapper auto-init**: Detect missing gradlew and offer to create it
4. **Cleaner warnings**: Filter out noise, highlight actual problems
5. **Quick start template**: `android create project --type=canvas` would be ideal

---

## Comparison to Expectations

### Expected (based on Android Studio experience)
1. Create project
2. Write code
3. Build → install → run
4. **Total time**: 5 minutes

### Actual (command-line first experience)
1. Create project structure manually
2. Configure Gradle (hit multiple issues)
3. Debug Java version
4. Debug signing
5. Generate keystore manually
6. Build → success
7. **Total time**: 30+ minutes

### Gap Analysis
The command-line Android development experience is significantly rougher than Android Studio. Many "automatic" conveniences are missing:
- No project templates
- Manual Gradle wrapper setup
- Manual keystore management
- Less helpful error messages
- No integrated troubleshooting

**Key Insight for PM**: The Android tooling is heavily optimized for Android Studio. Command-line workflows feel like an afterthought. This creates a high barrier for:
- CI/CD pipelines
- Alternative IDEs
- Scripting/automation
- Learning environments

---

## Recommendations

### Quick Wins
1. Auto-generate debug keystores in Gradle plugin
2. Improve Gradle error messages with actionable suggestions
3. Include gradle wrapper in SDK command-line tools
4. Add project templates to `sdkmanager`

### Longer Term
1. Create standalone Android CLI tool (like `flutter` or `cargo`)
2. Better version compatibility checking
3. Improve documentation for non-Android Studio workflows
4. Error message database with solutions
