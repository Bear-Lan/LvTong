import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { FBXLoader } from 'three/examples/jsm/loaders/FBXLoader.js' // 添加FBXLoader
import { DRACOLoader } from 'three/examples/jsm/loaders/DRACOLoader.js';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { TextureLoader } from 'three';
import { Ref, markRaw, onUnmounted, onMounted } from 'vue'

interface AnimatedModel {
    object: THREE.Group
    mixer: THREE.AnimationMixer
    clips: THREE.AnimationClip[]
    action?: THREE.AnimationAction;
}

interface MovingModel {
    currentSpeed: number
    name?: string
    object: THREE.Group
    speed: number
    dir: 1 | -1
    direction: 1 | -1
    minPos: THREE.Vector3
    maxPos: THREE.Vector3
    mode: 'bounce' | 'loop' | 'continuous'  // 添加 continuous 模式
    vector: THREE.Vector3
    onPositionReached?: (position: THREE.Vector3) => void
    isPaused?: boolean
    originalPosition?: THREE.Vector3
}

class ThreeViewer {
    private scene!: THREE.Scene
    private camera!: THREE.PerspectiveCamera
    private renderer!: THREE.WebGLRenderer
    private controls!: OrbitControls
    private animatedModels: AnimatedModel[] = []
    private movingModels: MovingModel[] = []
    private clock!: THREE.Clock
    private frameId!: number
    private fbxLoader!: FBXLoader // 添加FBXLoader实例
    private axesHelper?: THREE.AxesHelper;  // 添加坐标轴
    private textureLoader!: TextureLoader; // 声明属性
    private dracoLoader!: DRACOLoader;

    constructor(private container: Ref<HTMLDivElement | null>) {
        if (!container.value) {
            console.error('❌ initThree: container 未定义')
            return
        }

        this.scene = new THREE.Scene()
        this.camera = new THREE.PerspectiveCamera(60, container.value.clientWidth / container.value.clientHeight, 0.1, 1000)
        this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
        this.controls = new OrbitControls(this.camera, this.renderer.domElement)
        this.animatedModels = []
        this.movingModels = []
        this.clock = new THREE.Clock()
        this.frameId = 0
        this.fbxLoader = new FBXLoader() // 初始化FBXLoader
        this.dracoLoader = new DRACOLoader(); // 初始化DRACOLoader
        this.textureLoader = new TextureLoader();
        this.initializeScene()
        this.addResizeListener()
        this.startAnimationLoop()

        // this.showAxesHelper(50)   // 添加坐标轴
    }

    private async initializeScene() {
        //this.camera.position.set(-60, 60, -80)
        this.camera.position.set(-226.12, 72.09, -180.30)
        // 修改 FOV 让视场角更紧凑
        this.camera.fov = 15;
        this.camera.updateProjectionMatrix();

        // 调整 OrbitControls 的缩放范围
        // this.controls.minDistance = 10;
        // this.controls.maxDistance = 200;
        // this.camera.lookAt(300, 0, 0)
        this.renderer.setSize(this.container.value!.clientWidth, this.container.value!.clientHeight)
        this.container.value!.appendChild(this.renderer.domElement)

        const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
        const dirLight = new THREE.DirectionalLight(0xffffff, 1.2)
        dirLight.position.set(-100, 100, -100)

        this.scene.add(ambientLight, dirLight)

        this.controls.enableDamping = true
        this.controls.dampingFactor = 0.5
        this.controls.target.set(0, 1, 0)

        this.dracoLoader.setDecoderPath('/GreenChannel/libs/draco/'); // 请根据你的实际存放路径修改
        // 可选：预加载解码器，以便后续加载更快
        this.dracoLoader.preload();

        // 地面
        // const ground = this.createTexturedGround();
        // this.scene.add(await ground);


        this.controls.addEventListener('change', () => {
            this.renderer.render(this.scene, this.camera)
            // console.log('controls change');
        })
    }


    private addResizeListener() {
        if (!this.container.value) return

        // 使用 ResizeObserver 监听容器尺寸变化，更优雅
        const resizeObserver = new ResizeObserver(() => {
            if (!this.container.value) return
            this.camera.aspect = this.container.value.clientWidth / this.container.value.clientHeight
            this.camera.updateProjectionMatrix()
            this.renderer.setSize(this.container.value.clientWidth, this.container.value.clientHeight)
        })
        resizeObserver.observe(this.container.value)

        // 同时监听 window resize 作为备用
        window.addEventListener('resize', () => {
            if (!this.container.value) return
            this.camera.aspect = this.container.value.clientWidth / this.container.value.clientHeight
            this.camera.updateProjectionMatrix()
            this.renderer.setSize(this.container.value.clientWidth, this.container.value.clientHeight)
        })
    }

    private startAnimationLoop() {
        const animate = () => {
            this.frameId = requestAnimationFrame(animate)
            const delta = this.clock.getDelta()

            this.updateAnimatedModels(delta)
            this.updateMovingModels(delta)
            this.controls.update()
            this.renderer.render(this.scene, this.camera)
        }
        animate()
    }

    private updateAnimatedModels(delta: number) {
        const currentTime = performance.now();

        this.animatedModels.forEach((m, index) => {
            // 计算模型与相机的距离
            const distance = m.object.position.distanceTo(this.camera.position);

            // 为每个模型分配不同的更新相位
            const phaseOffset = index % 4;

            if (distance > 80) {
                // 很远：每4帧更新一次
                if (Math.floor(currentTime / 16) % 4 === phaseOffset) {
                    m.mixer.update(delta * 4);
                }
            } else if (distance > 40) {
                // 中等距离：每2帧更新一次
                if (Math.floor(currentTime / 16) % 2 === phaseOffset) {
                    m.mixer.update(delta * 2);
                }
            } else {
                // 近距离：每帧都更新
                m.mixer.update(delta);
            }
        });
    }

    private updateMovingModels(delta: number) {
        for (const mm of this.movingModels) {
            if (mm.isPaused) continue; // ← 关键，暂停就不动
            mm.object.position.addScaledVector(mm.vector, mm.speed * delta * mm.direction);
            mm.onPositionReached?.(mm.object.position.clone());
            // this.renderer.render(this.scene, this.camera)
            // 原来的边界处理逻辑可以保留
            if (mm.mode === 'bounce') {
                const toMax = new THREE.Vector3().subVectors(mm.maxPos, mm.object.position).dot(mm.vector);
                const toMin = new THREE.Vector3().subVectors(mm.minPos, mm.object.position).dot(mm.vector);
                if (toMax <= 0.001) mm.dir = -1;
                if (toMin >= 0.001) mm.dir = 1;
            } else if (mm.mode === 'loop') {
                const toMax = new THREE.Vector3().subVectors(mm.maxPos, mm.object.position).dot(mm.vector);
                if (toMax <= 0.001) mm.object.position.copy(mm.minPos);
            }
            // continuous 模式不需要边界处理
        }
    }

    /**
    * 显示坐标轴
    */
    public showAxesHelper(size: number = 5): void {
        if (this.axesHelper) {
            this.scene.remove(this.axesHelper);
        }

        this.axesHelper = new THREE.AxesHelper(size);
        this.scene.add(this.axesHelper);
    }

    public pauseModel(model: THREE.Group | string, speed: number = 0): void {
        const movingModel = this.findMovingModel(model);
        if (movingModel) {
            this.renderer.render(this.scene, this.camera)
            // 保存原始速度，如果还没保存
            if (movingModel.currentSpeed === undefined) {
                movingModel.currentSpeed = movingModel.speed;
            }
            movingModel.speed = speed; // 设置当前速度
        }
    }

    /**
     * 复位模型到起点
     */
    public resetModel(model: THREE.Group | string, position?: THREE.Vector3): void {
        const movingModel = this.findMovingModel(model);
        if (movingModel) {
            // debugger
            movingModel.object.position.copy(position ?? new THREE.Vector3(0, 0, 0));
        }
    }

    private findMovingModel(modelOrName: THREE.Group | string): MovingModel | undefined {
        if (typeof modelOrName === 'string') {
            return this.movingModels.find(m => m.name === modelOrName);
        } else {
            return this.movingModels.find(m => m.object === modelOrName);
        }
    }

    /**
     * 
     * @param model  模型
     * @param currentFrame  当前帧
     * @param targetFrame  目标帧
     * @param clipIndex     动画索引
     * @param fps           模型的动画帧率
     * @param duration      过渡时长
     * @returns Promise<void>
     * @returns  
     */
    public playSmoothTransition(model: THREE.Group, currentFrame: number, targetFrame: number, clipIndex: number = 0, fps: number = 60, duration: number = 1.0): Promise<void> {
        return new Promise((resolve) => {
            // 自动找到 AnimatedModel
            const animatedModel = this.animatedModels.find(m => m.object === model);
            if (!animatedModel) {
                console.warn("❌ 未找到 AnimatedModel");
                resolve();
                return;
            }

            const mixer = animatedModel.mixer;
            const clip = animatedModel.clips[clipIndex];
            if (!mixer || !clip) {
                console.warn("❌ mixer 或 clip 不存在");
                resolve();
                return;
            }

            // 复用 action（不会重复创建）
            let action = animatedModel.action;
            if (!action) {
                action = mixer.clipAction(clip);
                action.setLoop(THREE.LoopOnce, 1);
                action.clampWhenFinished = true;
                animatedModel.action = action;
            }

            // 当前时间点
            const startTime = currentFrame / fps;
            const targetTime = targetFrame / fps;

            action.time = startTime;
            action.paused = false;
            action.play();

            // 匀速速度 = 需要移动的时间 / duration
            const timeDiff = targetTime - startTime;
            mixer.timeScale = timeDiff / duration;

            // 到点锁定
            setTimeout(() => {
                mixer.timeScale = 0;
                action.time = targetTime;
                action.paused = true;
                resolve(); // 动画完成，解析 Promise
            }, duration * 1000);
        });
    }


    public addAnimatedGLTFModel(
        url: string,
        options?: {
            scale?: number
            position?: THREE.Vector3
            rotation?: THREE.Euler
            autoPlay?: boolean
            animationIndex?: number
            initialFrame?: number  // 新增：初始帧设置
            fps?: number
        },
        onLoaded?: (model: THREE.Group, mixer: THREE.AnimationMixer, clips: THREE.AnimationClip[]) => void
    ) {
        const loader = new GLTFLoader()
        loader.setDRACOLoader(this.dracoLoader);
        console.log('加载模型:', url);
        loader.load(url,
            (gltf) => {
                console.log('模型加载成功:', url);
                const model = markRaw(gltf.scene)
                setOptions(model, options)

                this.scene.add(model)
                this.renderer.render(this.scene, this.camera)
                const clips = gltf.animations
                let mixer: THREE.AnimationMixer | null = null
                if (clips && clips.length > 0) {
                    mixer = new THREE.AnimationMixer(model)

                const index = options?.animationIndex ?? 0
                const clip = clips[index]

                if (clip) {
                    const action = mixer.clipAction(clip)
                    const initialFrame = options?.initialFrame ?? 0
                    action.time = initialFrame / (options?.fps ?? 24)// 假设 60fps
                    // 关键修改：即使不自动播放，也要设置到正确的帧
                    if (options?.autoPlay) {
                        // 自动播放模式
                        action.setLoop(THREE.LoopRepeat, Infinity)
                        action.play()
                        action.paused = true // 立即暂停在指定帧
                    } else {
                        // 非自动播放模式：设置到指定帧并暂停
                        // action.play()
                        // action.paused = true // 立即暂停在指定帧
                        // action.stop()
                    }
                }

                this.animatedModels.push({ object: model, mixer, clips })
            }
            onLoaded?.(model, mixer!, clips)
            // console.log(`🎉 GLB 动画加载完成: ${url} 动画数量: ${clips.length}`)
        })
    }


    public moveModel(
        model: THREE.Group,
        speed: number = 0.02,
        minPos: THREE.Vector3 = new THREE.Vector3(-5, 0, 0),
        maxPos: THREE.Vector3 = new THREE.Vector3(5, 0, 0),
        mode: 'bounce' | 'loop' | 'continuous' = 'bounce',
        direction: 1 | -1 = 1,
        name?: string,
        onPositionReached?: (position: THREE.Vector3) => void
    ) {
        if (this.movingModels.find((m) => m.object === model)) return
        const vector = new THREE.Vector3().subVectors(maxPos, minPos).normalize()
        this.renderer.render(this.scene, this.camera)
        this.movingModels.push({ object: model, speed, dir: 1, direction, minPos, maxPos, mode, vector, name, onPositionReached, currentSpeed: speed })
    }

    public addLight(color: number = 0xffffff, intensity: number = 1) {
        const light = new THREE.PointLight(color, intensity)
        this.scene.add(light)
        return light
    }

    public getObjectCount() {
        return this.scene.children.length
    }

    /**
     * 销毁 ThreeViewer，释放资源
     */
    public dispose(): void {
        // 停止动画循环
        if (this.frameId) {
            cancelAnimationFrame(this.frameId)
        }

        // 清理渲染器
        if (this.renderer) {
            this.renderer.dispose()
            this.renderer.forceContextLoss()
            if (this.renderer.domElement && this.renderer.domElement.parentNode) {
                this.renderer.domElement.parentNode.removeChild(this.renderer.domElement)
            }
        }

        // 清理场景中的所有对象
        this.scene.traverse((object) => {
            if (object instanceof THREE.Mesh) {
                if (object.geometry) {
                    object.geometry.dispose()
                }
                if (object.material) {
                    if (Array.isArray(object.material)) {
                        object.material.forEach(m => m.dispose())
                    } else {
                        object.material.dispose()
                    }
                }
            }
        })

        // 清理控制器
        if (this.controls) {
            this.controls.dispose()
        }

        this.animatedModels = []
        this.movingModels = []
    }
}
// 应用配置选项
function setOptions(model: THREE.Group, options: any) {
    if (options) {
        if (options.scale) {
            model.scale.setScalar(options.scale)
        }
        if (options.position) {
            model.position.copy(options.position)
        }
        if (options.rotation) {
            model.rotation.copy(options.rotation)
        }
    }
}

export { ThreeViewer }
