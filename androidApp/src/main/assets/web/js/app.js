let currentPath = '';
let files = [];
let allFiles = [];
let viewMode = 'grid';
let sortBy = 'name';
let sortDir = 'asc';
let filterMode = 'all'; // all: 全部, files: 仅文件, folders: 仅文件夹
let currentEncoding = ''; // 当前编码，空表示自动检测
let selectedFile = null;
let selectedFiles = new Set(); // 多选文件集合
let selectionMode = false; // 多选模式开关
let lastSelectedIndex = -1; // 用于 Shift+Click 范围选择
let contextTarget = null;
let deleteTarget = null;
let renameTarget = null;
let previewPath = '';
let moveSourcePath = '';
let moveTargetPath = '';
const isReadOnly = location.pathname.endsWith('index.html') || location.pathname === '/' || location.pathname === '';

const FILE_TYPES = {
  image: {exts:['jpg','jpeg','png','gif','bmp','webp','svg','ico','tiff'], icon:'image', color:'#e74c3c'},
  video: {exts:['mp4','avi','mkv','mov','wmv','flv','webm','m4v'], icon:'movie', color:'#9b59b6'},
  audio: {exts:['mp3','wav','flac','aac','ogg','wma','m4a'], icon:'audiotrack', color:'#f39c12'},
  text:  {exts:['md','txt','log','csv','json','xml','yaml','yml','ini','cfg','conf','properties'], icon:'description', color:'#3498db'},
  code:  {exts:['gitignore','ets','kt','java','py','js','ts','html','css','php','c','cpp','h','go','rs','rb','sh','bat','sql','vue','jsx','tsx'], icon:'code', color:'#27ae60'},
  doc:   {exts:['doc','docx','xls','xlsx','ppt','pptx'], icon:'article', color:'#2980b9'},
  pdf:   {exts:['pdf'], icon:'picture_as_pdf', color:'#e74c3c'},
  zip:   {exts:['zip','rar','7z','tar','gz','bz2','xz'], icon:'folder_zip', color:'#f39c12'},
  exe:   {exts:['exe','msi','dmg','app','deb','rpm'], icon:'settings', color:'#7f8c8d'},
};

function getFileType(name) {
  if (!name) return null;
  const ext = name.split('.').pop().toLowerCase();
  for (const [type, cfg] of Object.entries(FILE_TYPES)) {
    if (cfg.exts.includes(ext)) return {type, ...cfg};
  }
  return {type: 'other', icon: 'insert_drive_file', color: '#7f8c8d'};
}

function getFileIcon(name, isFolder) {
  if (isFolder) return {icon: 'folder', color: '#F59E0B'};
  return getFileType(name);
}

function formatSize(bytes) {
  if (bytes === 0) return '—';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return (bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0) + ' ' + units[i];
}

function formatDate(ts) {
  if (!ts) return '—';
  const d = new Date(ts);
  const now = new Date();
  const diff = now - d;
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前';
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function toast(msg, type = 'success') {
  const container = document.getElementById('toastContainer');
  const el = document.createElement('div');
  el.className = 'toast ' + type;
  el.innerHTML = `<span class="material-icons">${type === 'success' ? 'check_circle' : 'error'}</span><span>${escapeHtml(msg)}</span>`;
  container.appendChild(el);
  setTimeout(() => {
    el.style.opacity = '0';
    el.style.transform = 'translateX(40px)';
    el.style.transition = 'all .3s';
    setTimeout(() => el.remove(), 300);
  }, 3000);
}

function api(url, params = {}) {
  const qs = new URLSearchParams(params).toString();
  const fullUrl = qs ? `/main/${url}?${qs}` : `/main/${url}`;
  return fetch(fullUrl).then(r => r.text());
}

function apiPost(url, body = {}) {
  return fetch(`/main/${url}`, {
    method: 'POST',
    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
    body: new URLSearchParams(body).toString()
  }).then(r => r.text());
}

function navigateTo(path) {
  const newPath = path || '';
  if (newPath === currentPath) {
    loadFiles();
    return;
  }
  const url = newPath ? window.location.pathname + '?path=' + encodeURIComponent(newPath) : window.location.pathname;
  window.location.href = url;
}

function loadState() {
  const urlParams = new URLSearchParams(window.location.search);
  const urlPath = urlParams.get('path');
  if (urlPath) {
    currentPath = decodeURIComponent(urlPath);
  }
  // 从 localStorage 恢复状态
  const savedView = localStorage.getItem('fm_viewMode');
  if (savedView) viewMode = savedView;
  const savedSort = localStorage.getItem('fm_sortBy');
  if (savedSort) sortBy = savedSort;
  const savedDir = localStorage.getItem('fm_sortDir');
  if (savedDir) sortDir = savedDir;
  const savedFilter = localStorage.getItem('fm_filterMode');
  if (savedFilter) filterMode = savedFilter;
}

function saveViewState() {
  localStorage.setItem('fm_viewMode', viewMode);
}
function saveSortState() {
  localStorage.setItem('fm_sortBy', sortBy);
  localStorage.setItem('fm_sortDir', sortDir);
}
function saveFilterState() {
  localStorage.setItem('fm_filterMode', filterMode);
}

function loadFiles() {
  const params = currentPath ? {path: currentPath} : {};
  api('getAllFile', params).then(text => {
    try {
      allFiles = JSON.parse(text);
    } catch (e) {
      allFiles = [];
    }
    applyFilters();
    renderBreadcrumb();
  }).catch(() => {
    allFiles = [];
    applyFilters();
  });
}

function applyFilters() {
  const query = document.getElementById('searchInput').value.toLowerCase().trim();
  let filtered = allFiles;
  
  // 按类型筛选
  if (filterMode === 'files') {
    filtered = filtered.filter(f => !f.isFolder);
  } else if (filterMode === 'folders') {
    filtered = filtered.filter(f => f.isFolder);
  }
  
  // 按名称搜索
  if (query) {
    filtered = filtered.filter(f => f.name.toLowerCase().includes(query));
  }
  files = sortFileList(filtered);
  renderFiles();
}

function changeFilterMode(mode) {
  filterMode = mode;
  document.querySelectorAll('.filter-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.mode === mode);
  });
  saveFilterState();
  applyFilters();
}

function sortFiles() {
  const sort = document.getElementById('sortSelect').value;
  const [key, dir] = sort.split('-');
  sortBy = key;
  sortDir = dir;
  saveSortState();
  applyFilters();
}

function sortFileList(list) {
  const sort = document.getElementById('sortSelect').value;
  const [key, dir] = sort.split('-');
  return [...list].sort((a, b) => {
    if (a.isFolder !== b.isFolder) return a.isFolder ? -1 : 1;
    let cmp = 0;
    if (key === 'name') cmp = a.name.localeCompare(b.name, 'zh-CN');
    else if (key === 'size') cmp = a.length - b.length;
    else if (key === 'date') cmp = a.lastModified - b.lastModified;
    return dir === 'desc' ? -cmp : cmp;
  });
}

function renderBreadcrumb() {
  const bc = document.getElementById('breadcrumb');
  let html = `<div class="breadcrumb-item ${currentPath ? '' : 'active'}" onclick="navigateTo('')">
    <span class="material-icons" style="font-size:18px;margin-right:2px">home</span>根目录
  </div>`;
  if (currentPath) {
    const parts = currentPath.split('/').filter(Boolean);
    let accPath = '';
    parts.forEach((part, i) => {
      accPath += (accPath ? '/' : '') + part;
      const isLast = i === parts.length - 1;
      html += `<span class="breadcrumb-sep"><span class="material-icons" style="font-size:14px">chevron_right</span></span>`;
      html += `<div class="breadcrumb-item ${isLast ? 'active' : ''}" onclick="navigateTo('${escapeHtml(accPath)}')">${escapeHtml(part)}</div>`;
    });
  }
  bc.innerHTML = html;
}

function renderFiles() {
  const content = document.getElementById('content');
  const countEl = document.getElementById('fileCount');
  const folderCount = files.filter(f => f.isFolder).length;
  const fileCount = files.filter(f => !f.isFolder).length;
  const totalFolders = allFiles.filter(f => f.isFolder).length;
  const totalFiles = allFiles.filter(f => !f.isFolder).length;
  const parts = [];
  
  if (folderCount > 0) parts.push(folderCount + ' 个文件夹');
  if (fileCount > 0) parts.push(fileCount + ' 个文件');
  
  let countText = parts.length ? parts.join('，') : '空目录';
  if (filterMode !== 'all') {
    countText += ` (共 ${totalFolders} 文件夹, ${totalFiles} 文件)`;
  }
  countEl.textContent = countText;

  if (files.length === 0) {
    content.innerHTML = `<div class="empty-state">
      <span class="material-icons">folder_open</span>
      <p>此目录为空</p>
      <span>拖拽文件到此处上传，或点击上方"上传"按钮</span>
    </div>`;
    return;
  }

  if (viewMode === 'grid') {
    renderGridView(content);
  } else if (viewMode === 'preview') {
    renderPreviewView(content);
  } else {
    renderListView(content);
  }
}

function renderGridView(container) {
  let html = '<div class="file-grid">';
  files.forEach((f, idx) => {
    const fi = getFileIcon(f.name, f.isFolder);
    const nameClass = f.isFolder ? ' folder-name' : '';
    const isSelected = selectedFiles.has(f.name);
    const checkboxHtml = selectionMode ? `<input type="checkbox" class="file-checkbox" ${isSelected ? 'checked' : ''} onclick="event.stopPropagation();toggleFileSelection('${escapeHtml(f.name)}', ${idx}, event)">` : '';
    const clickHandler = selectionMode ? `handleFileClick('${escapeHtml(f.name)}', ${idx}, event)` : (f.isFolder ? `navigateTo('${escapeHtml(currentPath ? currentPath + '/' + f.name : f.name)}')` : `previewOrOpenFile('${escapeHtml(f.name)}')`);
    html += `<div class="file-card ${isSelected ? 'selected' : ''}" 
      onclick="${clickHandler}"
      oncontextmenu="showItemMenu(event,'${escapeHtml(f.name)}',${f.isFolder})"
      data-name="${escapeHtml(f.name)}" data-index="${idx}">
      <div class="file-card-checkbox" style="visibility:${selectionMode ? 'visible' : 'hidden'}">${checkboxHtml}</div>
      <span class="material-icons file-icon" style="color:${fi.color}">${fi.icon}</span>
      <div class="file-name${nameClass}">${escapeHtml(f.name)}</div>
      <div class="file-meta">${f.isFolder ? '—' : formatSize(f.length)}</div>
      <div class="file-meta">${formatDate(f.lastModified)}</div>
    </div>`;
  });
  html += '</div>';
  container.innerHTML = html;
}

function renderPreviewView(container) {
  let html = '<div class="file-preview-grid">';
  files.forEach((f, idx) => {
    const fi = getFileIcon(f.name, f.isFolder);
    const nameClass = f.isFolder ? ' folder-name' : '';
    const isSelected = selectedFiles.has(f.name);
    const checkboxHtml = selectionMode ? `<input type="checkbox" class="file-checkbox" ${isSelected ? 'checked' : ''} onclick="event.stopPropagation();toggleFileSelection('${escapeHtml(f.name)}', ${idx}, event)">` : '';
    const clickHandler = selectionMode ? `handleFileClick('${escapeHtml(f.name)}', ${idx}, event)` : (f.isFolder ? `navigateTo('${escapeHtml(currentPath ? currentPath + '/' + f.name : f.name)}')` : `previewOrOpenFile('${escapeHtml(f.name)}')`);
    
    // 判断是否为图片或视频
    const ext = f.name.split('.').pop().toLowerCase();
    const isImage = ['jpg','jpeg','png','gif','bmp','webp','svg','ico'].includes(ext);
    const isVideo = ['mp4','avi','mkv','mov','wmv','flv','webm','m4v'].includes(ext);
    
    let thumbnailHtml = '';
    if (isImage) {
      const filePath = currentPath ? currentPath + '/' + f.name : f.name;
      thumbnailHtml = `<img class="preview-thumbnail lazy-media" data-src="/main/files/${encodeURIComponent(filePath).replace(/%2F/g, '/')}" alt="${escapeHtml(f.name)}" onerror="this.style.display='none'">`;
    } else if (isVideo) {
      const filePath = currentPath ? currentPath + '/' + f.name : f.name;
      thumbnailHtml = `<div class="video-placeholder">
        <span class="material-icons video-loading-icon">movie</span>
        <video class="preview-thumbnail lazy-video" data-src="/main/files/${encodeURIComponent(filePath).replace(/%2F/g, '/')}" preload="metadata" muted onloadeddata="this.style.opacity='1';this.previousElementSibling.style.display='none'" onemptied="this.style.opacity='0';this.previousElementSibling.style.display='block'"></video>
      </div>`;
    }
    
    html += `<div class="file-preview-card ${isSelected ? 'selected' : ''}" 
      onclick="${clickHandler}"
      oncontextmenu="showItemMenu(event,'${escapeHtml(f.name)}',${f.isFolder})"
      data-name="${escapeHtml(f.name)}" data-index="${idx}">
      <div class="file-preview-checkbox" style="visibility:${selectionMode ? 'visible' : 'hidden'}">${checkboxHtml}</div>
      <div class="preview-content">
        ${thumbnailHtml || `<span class="material-icons preview-icon" style="color:${fi.color}">${fi.icon}</span>`}
      </div>
      <div class="file-name${nameClass}">${escapeHtml(f.name)}</div>
      <div class="file-meta">${f.isFolder ? '—' : formatSize(f.length)}</div>
      <div class="file-meta">${formatDate(f.lastModified)}</div>
    </div>`;
  });
  html += '</div>';
  container.innerHTML = html;
  initLazyLoad();
}

// 懒加载初始化
let lazyObserver = null;

function initLazyLoad() {
  // 懒加载观察器 - 进入可视区域时加载
  if (!lazyObserver) {
    lazyObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        const el = entry.target;
        if (entry.isIntersecting) {
          // 进入可视区域 - 加载媒体
          const src = el.dataset.src;
          if (src) {
            el.src = src;
            el.dataset.loaded = 'true';
            delete el.dataset.src;
          }
        } else if (el.tagName === 'VIDEO') {
          // 离开可视区域 - 仅卸载视频释放内存（图片保持加载）
          if (el.dataset.loaded === 'true' && !el.classList.contains('playing')) {
            const currentSrc = el.src;
            if (currentSrc) {
              el.dataset.src = currentSrc;
              el.src = '';
              el.dataset.loaded = 'false';
              el.pause();
              el.load();
            }
          }
        }
      });
    }, { rootMargin: '200px' }); // 提前200px加载，延迟200px卸载
  }
  // 图片懒加载
  document.querySelectorAll('.lazy-media[data-src]').forEach(el => {
    lazyObserver.observe(el);
  });
  // 视频懒加载
  document.querySelectorAll('.lazy-video[data-src]').forEach(el => {
    lazyObserver.observe(el);
  });
}

function renderListView(container) {
  let html = `<div class="file-list">
    <div class="file-list-header">
      <span class="list-checkbox-header" style="visibility:${selectionMode ? 'visible' : 'hidden'}"></span>
      <span>名称</span><span>大小</span><span>修改时间</span><span style="text-align:center">操作</span>
    </div>`;
  files.forEach((f, idx) => {
    const fi = getFileIcon(f.name, f.isFolder);
    const nameClass = f.isFolder ? ' folder-name' : '';
    const isSelected = selectedFiles.has(f.name);
    const checkboxHtml = selectionMode ? `<input type="checkbox" class="file-checkbox" ${isSelected ? 'checked' : ''} onclick="event.stopPropagation();toggleFileSelection('${escapeHtml(f.name)}', ${idx}, event)">` : '';
    const clickHandler = selectionMode ? `handleFileClick('${escapeHtml(f.name)}', ${idx}, event)` : (f.isFolder ? `navigateTo('${escapeHtml(currentPath ? currentPath + '/' + f.name : f.name)}')` : `previewOrOpenFile('${escapeHtml(f.name)}')`);
    html += `<div class="file-list-row ${isSelected ? 'selected' : ''}"
      onclick="${clickHandler}"
      oncontextmenu="showItemMenu(event,'${escapeHtml(f.name)}',${f.isFolder})"
      data-name="${escapeHtml(f.name)}" data-index="${idx}">
      <div class="file-checkbox-cell" style="visibility:${selectionMode ? 'visible' : 'hidden'}">${checkboxHtml}</div>
      <div class="file-name-cell">
        <span class="material-icons" style="color:${fi.color}">${fi.icon}</span>
        <span class="file-name${nameClass}">${escapeHtml(f.name)}</span>
      </div>
      <span class="file-size">${f.isFolder ? '—' : formatSize(f.length)}</span>
      <span class="file-date">${formatDate(f.lastModified)}</span>
      <div class="file-actions-cell">
        ${f.isFolder 
          ? `<button class="btn btn-ghost" onclick="event.stopPropagation();navigateTo('${escapeHtml(currentPath ? currentPath + '/' + f.name : f.name)}')" title="打开"><span class="material-icons" style="font-size:16px">folder_open</span></button>
             <button class="btn btn-ghost" onclick="event.stopPropagation();copyFolderLink('${escapeHtml(f.name)}')" title="复制链接"><span class="material-icons" style="font-size:16px">link</span></button>
             <button class="btn btn-ghost" onclick="event.stopPropagation();openFolderInNewTab('${escapeHtml(f.name)}')" title="在新标签页打开"><span class="material-icons" style="font-size:16px">open_in_new</span></button>`
          : `<button class="btn btn-ghost" onclick="event.stopPropagation();previewFile('${escapeHtml(f.name)}')" title="预览"><span class="material-icons" style="font-size:16px">visibility</span></button>
             <button class="btn btn-ghost" onclick="event.stopPropagation();downloadFile('${escapeHtml(f.name)}')" title="下载"><span class="material-icons" style="font-size:16px">download</span></button>
             <button class="btn btn-ghost" onclick="event.stopPropagation();copyLink('${escapeHtml(f.name)}')" title="复制链接"><span class="material-icons" style="font-size:16px">link</span></button>
             <button class="btn btn-ghost" onclick="event.stopPropagation();openInNewTab('${escapeHtml(f.name)}')" title="在新标签页打开"><span class="material-icons" style="font-size:16px">open_in_new</span></button>`
        }
        ${!isReadOnly ? `
        <button class="btn btn-ghost" onclick="event.stopPropagation();showRenameModal('${escapeHtml(f.name)}')" title="重命名"><span class="material-icons" style="font-size:16px">edit</span></button>
        <button class="btn btn-ghost" onclick="event.stopPropagation();showDeleteModal('${escapeHtml(f.name)}',${f.isFolder})" title="删除"><span class="material-icons" style="font-size:16px;color:var(--danger)">delete</span></button>
        ` : ''}
      </div>
    </div>`;
  });
  html += '</div>';
  container.innerHTML = html;
}

function selectFile(e, name) {
  e.stopPropagation();
  selectedFile = selectedFile === name ? null : name;
  document.querySelectorAll('.file-card,.file-list-row').forEach(el => {
    el.classList.toggle('selected', el.dataset.name === selectedFile);
  });
}

function handleFileClick(name, idx, e) {
  if (e.ctrlKey || e.metaKey) {
    toggleFileSelection(name, idx, e);
  } else if (e.shiftKey && lastSelectedIndex !== -1) {
    e.preventDefault();
    const start = Math.min(lastSelectedIndex, idx);
    const end = Math.max(lastSelectedIndex, idx);
    for (let i = start; i <= end; i++) {
      selectedFiles.add(files[i].name);
    }
    lastSelectedIndex = idx;
    updateSelectionUI();
  } else if (selectionMode) {
    // 多选模式下，普通点击也切换选中状态
    toggleFileSelection(name, idx, e);
  } else {
    const f = files.find(f => f.name === name);
    if (!f) return;
    if (f.isFolder) {
      navigateTo(currentPath ? currentPath + '/' + f.name : f.name);
    } else {
      previewOrOpenFile(f.name);
    }
  }
}

function toggleFileSelection(name, idx, e) {
  if (e.shiftKey && lastSelectedIndex !== -1) {
    // Shift+Click with checkbox
    const start = Math.min(lastSelectedIndex, idx);
    const end = Math.max(lastSelectedIndex, idx);
    for (let i = start; i <= end; i++) {
      selectedFiles.add(files[i].name);
    }
  } else {
    // 切换单个文件的选择状态
    if (selectedFiles.has(name)) {
      selectedFiles.delete(name);
    } else {
      selectedFiles.add(name);
    }
  }
  lastSelectedIndex = idx;
  updateSelectionUI();
}

function updateSelectionUI() {
  // 更新复选框状态
  document.querySelectorAll('.file-card,.file-preview-card,.file-list-row').forEach(el => {
    const isSelected = selectedFiles.has(el.dataset.name);
    el.classList.toggle('selected', isSelected);
    const checkbox = el.querySelector('.file-checkbox');
    if (checkbox) checkbox.checked = isSelected;
  });
  
  // 更新选择工具栏
  const toolbar = document.getElementById('selectionToolbar');
  const countEl = document.getElementById('selectionCount');
  if (selectedFiles.size > 0) {
    toolbar.style.display = 'flex';
    countEl.textContent = `已选择 ${selectedFiles.size} 个项目`;
  } else {
    toolbar.style.display = 'none';
  }
}

function selectAll() {
  files.forEach(f => selectedFiles.add(f.name));
  lastSelectedIndex = files.length - 1;
  updateSelectionUI();
}

function toggleSelectionMode() {
  selectionMode = !selectionMode;
  const btn = document.getElementById('selectModeBtn');
  btn.classList.toggle('active', selectionMode);
  
  if (!selectionMode) {
    // 退出多选模式时清除选择
    deselectAll();
  }
  
  // 重新渲染文件以显示/隐藏复选框
  renderFiles();
}

function deselectAll() {
  selectedFiles.clear();
  lastSelectedIndex = -1;
  updateSelectionUI();
}

function setView(mode) {
  viewMode = mode;
  document.getElementById('viewGrid').classList.toggle('active', mode === 'grid');
  const viewPreviewBtn = document.getElementById('viewPreview');
  if (viewPreviewBtn) {
    viewPreviewBtn.classList.toggle('active', mode === 'preview');
  }
  document.getElementById('viewList').classList.toggle('active', mode === 'list');
  saveViewState();
  renderFiles();
}

function filterFiles() {
  applyFilters();
}

function hideMenu() {
  document.getElementById('contextMenu').classList.remove('show');
}

function showItemMenu(e, name, isFolder) {
  e.preventDefault();
  e.stopPropagation();
  contextTarget = {name, isFolder};
  const menu = document.getElementById('contextMenu');
  let html = '';
  if (isFolder) {
    html += `<div class="context-menu-item" onclick="hideMenu();navigateTo('${escapeHtml(currentPath ? currentPath + '/' + name : name)}')">
      <span class="material-icons">folder_open</span>打开</div>`;
    html += `<div class="context-menu-item" onclick="hideMenu();copyFolderLink('${escapeHtml(name)}')">
      <span class="material-icons">link</span>复制链接</div>`;
    html += `<div class="context-menu-item" onclick="hideMenu();openFolderInNewTab('${escapeHtml(name)}')">
      <span class="material-icons">open_in_new</span>在新标签页打开</div>`;
  } else {
    html += `<div class="context-menu-item" onclick="hideMenu();previewOrOpenFile('${escapeHtml(name)}')">
      <span class="material-icons">visibility</span>预览</div>`;
    html += `<div class="context-menu-item" onclick="hideMenu();downloadFile('${escapeHtml(name)}')">
      <span class="material-icons">download</span>下载</div>`;
    html += `<div class="context-menu-item" onclick="hideMenu();copyLink('${escapeHtml(name)}')">
      <span class="material-icons">link</span>复制下载链接</div>`;
    html += `<div class="context-menu-item" onclick="hideMenu();openInNewTab('${escapeHtml(name)}')">
      <span class="material-icons">open_in_new</span>在新标签页打开</div>`;
  }
  if (!isReadOnly) {
    html += '<div class="context-menu-sep"></div>';
    html += `<div class="context-menu-item" onclick="hideMenu();showRenameModal('${escapeHtml(name)}')">
      <span class="material-icons">edit</span>重命名</div>`;
    html += `<div class="context-menu-item" onclick="hideMenu();showMoveModal('${escapeHtml(name)}')">
      <span class="material-icons">folder_open</span>移动</div>`;
    html += '<div class="context-menu-sep"></div>';
    html += `<div class="context-menu-item danger" onclick="hideMenu();showDeleteModal('${escapeHtml(name)}',${isFolder})">
      <span class="material-icons">delete</span>删除</div>`;
  }
  menu.innerHTML = html;
  menu.classList.add('show');
  
  // 获取菜单实际尺寸后调整位置
  const menuRect = menu.getBoundingClientRect();
  const menuWidth = menuRect.width;
  const menuHeight = menuRect.height;
  
  let left = e.clientX;
  let top = e.clientY;
  
  // 右边界检查
  if (left + menuWidth > window.innerWidth) {
    left = window.innerWidth - menuWidth - 10;
  }
  // 下边界检查
  if (top + menuHeight > window.innerHeight) {
    top = window.innerHeight - menuHeight - 10;
  }
  // 左边界检查
  if (left < 0) left = 10;
  // 上边界检查
  if (top < 0) top = 10;
  
  menu.style.left = left + 'px';
  menu.style.top = top + 'px';
}

function downloadFile(name) {
  const path = currentPath ? currentPath + '/' + name : name;
  const url = '/main/files/' + encodeURIComponent(path).replace(/%2F/g, '/');
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

function getFileUrl(name) {
  const path = currentPath ? currentPath + '/' + name : name;
  return window.location.origin + '/main/files/' + encodeURIComponent(path).replace(/%2F/g, '/');
}

function copyToClipboard(text) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => {
      toast('链接已复制到剪贴板');
    }).catch(() => {
      fallbackCopy(text);
    });
  } else {
    fallbackCopy(text);
  }
}

function fallbackCopy(text) {
  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.style.position = 'fixed';
  textarea.style.left = '-9999px';
  document.body.appendChild(textarea);
  textarea.select();
  try {
    document.execCommand('copy');
    toast('链接已复制到剪贴板');
  } catch (err) {
    toast('复制失败: ' + text, 'error');
  }
  document.body.removeChild(textarea);
}

function copyLink(name) {
  const url = getFileUrl(name);
  copyToClipboard(url);
}

function openInNewTab(name) {
  const url = getFileUrl(name);
  window.open(url, '_blank');
}

function getFolderUrl(name) {
  const path = currentPath ? currentPath + '/' + name : name;
  return window.location.origin + window.location.pathname + '?path=' + encodeURIComponent(path);
}

function copyFolderLink(name) {
  const url = getFolderUrl(name);
  copyToClipboard(url);
}

function openFolderInNewTab(name) {
  const url = getFolderUrl(name);
  window.open(url, '_blank');
}

function previewOrOpenFile(name) {
  const ext = name.split('.').pop().toLowerCase();
  if (ext === 'html') {
    openInNewTab(name);
    return;
  }
  const ft = getFileType(name);
  if (ft && (ft.type === 'text' || ft.type === 'code')) {
    previewPath = currentPath ? currentPath + '/' + name : name;
    if (isReadOnly) {
      previewFile(name);
    } else {
      editFile();
    }
    return;
  }
  if (ft && (ft.type === 'image' || ft.type === 'audio')) {
    previewFile(name);
  } else {
    openInNewTab(name);
  }
}

function previewFile(name) {
  previewPath = currentPath ? currentPath + '/' + name : name;
  const modal = document.getElementById('previewModal');
  const title = document.getElementById('previewTitle');
  const body = document.getElementById('previewBody');
  const editBtn = document.getElementById('editButton');
  const encodingWrap = document.getElementById('previewEncodingWrap');
  const encodingSelect = document.getElementById('previewEncoding');
  title.textContent = name;
  body.innerHTML = '';

  const ext = name.split('.').pop().toLowerCase();
  const ft = getFileType(name);
  const isTextFile = ft && (ft.type === 'text' || ft.type === 'code');

  if (editBtn) {
    editBtn.style.display = isTextFile ? 'flex' : 'none';
  }

  // 显示/隐藏编码选择器
  if (encodingWrap) {
    encodingWrap.style.display = isTextFile ? 'flex' : 'none';
  }
  if (encodingSelect && currentEncoding) {
    encodingSelect.value = currentEncoding;
  }

  if (ft && ft.type === 'image') {
    const container = document.createElement('div');
    container.className = 'image-container';
    const img = document.createElement('img');
    img.src = '/main/files/' + encodeURIComponent(previewPath).replace(/%2F/g, '/');
    img.alt = name;
    container.appendChild(img);
    body.appendChild(container);
  } else if (ft && ft.type === 'video') {
    const container = document.createElement('div');
    container.className = 'preview-container';
    const vid = document.createElement('video');
    vid.src = '/main/files/' + encodeURIComponent(previewPath).replace(/%2F/g, '/');
    vid.controls = true;
    vid.autoplay = true;
    vid.style.maxWidth = '100%';
    container.appendChild(vid);
    body.appendChild(container);
  } else if (ft && ft.type === 'audio') {
    const container = document.createElement('div');
    container.className = 'preview-container';
    const aud = document.createElement('audio');
    aud.src = '/main/files/' + encodeURIComponent(previewPath).replace(/%2F/g, '/');
    aud.controls = true;
    aud.autoplay = true;
    container.appendChild(aud);
    body.appendChild(container);
  } else if (isTextFile) {
    loadPreviewText();
  } else if (ext === 'pdf') {
    const iframe = document.createElement('iframe');
    iframe.src = '/main/files/' + encodeURIComponent(previewPath).replace(/%2F/g, '/');
    iframe.style.cssText = 'width:100%;height:100%;min-height:500px;border:none';
    body.appendChild(iframe);
  } else {
    body.innerHTML = `<div style="text-align:center;padding:40px;color:var(--text-secondary)">
      <span class="material-icons" style="font-size:48px;color:var(--text-light);display:block;margin-bottom:12px">preview</span>
      <p>不支持预览此文件类型</p>
      <button class="btn btn-primary btn-sm" style="margin-top:12px" onclick="downloadPreviewFile()">
        <span class="material-icons">download</span>下载文件
      </button>
    </div>`;
  }
  modal.classList.add('show');
}

function loadPreviewText(encoding) {
  const body = document.getElementById('previewBody');
  const params = {path: previewPath};
  if (encoding) params.encoding = encoding;
  
  api('readText', params).then(text => {
    const textarea = document.createElement('textarea');
    textarea.className = 'preview-textarea';
    textarea.value = text;
    textarea.readOnly = true;
    body.appendChild(textarea);
  }).catch(() => {
    body.innerHTML = '<p style="padding:40px;color:var(--text-secondary)">无法预览此文件</p>';
  });
}

function reloadPreviewText() {
  const select = document.getElementById('previewEncoding');
  const body = document.getElementById('previewBody');
  currentEncoding = select ? select.value : '';
  
  // 移除旧的 textarea 元素
  const oldTextarea = body.querySelector('textarea');
  if (oldTextarea) oldTextarea.remove();
  
  loadPreviewText(currentEncoding);
}

function editFile() {
  const name = previewPath.split('/').pop();
  const params = {path: previewPath};
  if (currentEncoding) params.encoding = currentEncoding;
  
  api('readText', params).then(text => {
    document.getElementById('editorTitle').textContent = '编辑: ' + name;
    document.getElementById('editorTextarea').value = text;
    
    // 更新编码选择器
    const editorEncoding = document.getElementById('editorEncoding');
    if (editorEncoding) {
      editorEncoding.value = currentEncoding;
    }
    
    closeModal('previewModal');
    document.getElementById('editorModal').classList.add('show');
  }).catch(() => {
    toast('无法读取文件内容', 'error');
  });
}

function reloadEditorText() {
  const select = document.getElementById('editorEncoding');
  const encoding = select ? select.value : '';
  currentEncoding = encoding;
  editFile();
}

function saveFile() {
  const content = document.getElementById('editorTextarea').value;
  apiPost('writeText', {path: previewPath, txt: content}).then(result => {
    if (result.includes('成功')) {
      toast('保存成功');
      closeModal('editorModal');
      loadFiles();
    } else {
      toast(result, 'error');
    }
  }).catch(() => toast('保存失败', 'error'));
}

function showMoveModal(name) {
  moveSourcePath = currentPath ? currentPath + '/' + name : name;
  document.getElementById('moveSource').textContent = moveSourcePath;
  moveTargetPath = '';
  loadFolderTree('');
  document.getElementById('moveModal').classList.add('show');
}

function loadFolderTree(dirPath) {
  api('getAllFile', {path: dirPath}).then(text => {
    let data = [];
    try { data = JSON.parse(text); } catch (e) { data = []; }
    const tree = document.getElementById('folderTree');
    const selectedAttr = moveTargetPath === '' ? ' selected' : '';
    let html = `<div class="folder-tree-item${selectedAttr}" data-path="" onclick="selectMoveTarget('')">
      <span class="material-icons" style="font-size:16px;color:#6B7280">home</span>
      <span>根目录</span>
    </div>`;
    const folders = data.filter(f => f.isFolder);
    folders.forEach(f => {
      const fullPath = dirPath ? dirPath + '/' + f.name : f.name;
      const isSelected = moveTargetPath === fullPath;
      html += `<div class="folder-tree-item${isSelected ? ' selected' : ''}" data-path="${escapeHtml(fullPath)}" onclick="selectMoveTarget('${escapeHtml(fullPath)}')">
        <span class="material-icons expand-icon" style="font-size:14px;color:#9CA3AF;cursor:pointer" onclick="event.stopPropagation();toggleFolder(this,'${escapeHtml(fullPath)}')">chevron_right</span>
        <span class="material-icons" style="font-size:16px;color:#F59E0B">folder</span>
        <span>${escapeHtml(f.name)}</span>
      </div>`;
    });
    tree.innerHTML = html;
  });
}

function toggleFolder(icon, dirPath) {
  const item = icon.closest('.folder-tree-item');
  let childContainer = item.nextElementSibling;
  if (childContainer && childContainer.classList.contains('folder-tree-children')) {
    const isOpen = icon.textContent === 'expand_more';
    icon.textContent = isOpen ? 'chevron_right' : 'expand_more';
    childContainer.style.display = isOpen ? 'none' : 'block';
    return;
  }
  icon.textContent = 'expand_more';
  icon.classList.add('loading');
  api('getAllFile', {path: dirPath}).then(text => {
    icon.classList.remove('loading');
    let data = [];
    try { data = JSON.parse(text); } catch (e) { data = []; }
    const folders = data.filter(f => f.isFolder);
    if (folders.length === 0) {
      icon.textContent = '';
      icon.style.visibility = 'hidden';
      return;
    }
    const childDiv = document.createElement('div');
    childDiv.className = 'folder-tree-children';
    let childHtml = '';
    folders.forEach(f => {
      const fullPath = dirPath + '/' + f.name;
      const isSelected = moveTargetPath === fullPath;
      childHtml += `<div class="folder-tree-item${isSelected ? ' selected' : ''}" style="padding-left:24px" data-path="${escapeHtml(fullPath)}" onclick="selectMoveTarget('${escapeHtml(fullPath)}')">
        <span class="material-icons expand-icon" style="font-size:14px;color:#9CA3AF;cursor:pointer" onclick="event.stopPropagation();toggleFolder(this,'${escapeHtml(fullPath)}')">chevron_right</span>
        <span class="material-icons" style="font-size:16px;color:#F59E0B">folder</span>
        <span>${escapeHtml(f.name)}</span>
      </div>`;
    });
    childDiv.innerHTML = childHtml;
    item.after(childDiv);
  });
}

function selectMoveTarget(path) {
  moveTargetPath = path;
  document.querySelectorAll('.folder-tree-item').forEach(el => el.classList.remove('selected'));
  document.querySelector(`.folder-tree-item[data-path="${escapeHtml(path)}"]`)?.classList.add('selected');
}

function confirmMove() {
  apiPost('move', {path: moveSourcePath, targetDir: moveTargetPath}).then(result => {
    if (result.includes('成功')) {
      toast('移动成功');
      closeModal('moveModal');
      loadFiles();
    } else {
      toast(result, 'error');
    }
  }).catch(() => toast('移动失败', 'error'));
}

function downloadPreviewFile() {
  if (previewPath) {
    const name = previewPath.split('/').pop();
    downloadFile(name);
  }
}

function showMkdirModal() {
  document.getElementById('mkdirInput').value = '';
  document.getElementById('mkdirModal').classList.add('show');
  setTimeout(() => document.getElementById('mkdirInput').focus(), 100);
}

function createFolder() {
  const name = document.getElementById('mkdirInput').value.trim();
  if (!name) { toast('请输入文件夹名称', 'error'); return; }
  apiPost('mkdir', {path: currentPath, name: name}).then(result => {
    if (result.includes('成功')) {
      toast('文件夹创建成功');
      closeModal('mkdirModal');
      loadFiles();
    } else {
      toast(result, 'error');
    }
  }).catch(() => toast('创建失败', 'error'));
}

function showCreateFileModal() {
  document.getElementById('createFileInput').value = '';
  document.getElementById('createFileModal').classList.add('show');
  setTimeout(() => document.getElementById('createFileInput').focus(), 100);
}

function createFile() {
  const name = document.getElementById('createFileInput').value.trim();
  if (!name) { toast('请输入文件名', 'error'); return; }
  apiPost('createFile', {path: currentPath, name: name}).then(result => {
    if (result.includes('成功')) {
      toast('文件创建成功');
      closeModal('createFileModal');
      loadFiles();
    } else {
      toast(result, 'error');
    }
  }).catch(() => toast('创建失败', 'error'));
}

function showRenameModal(name) {
  renameTarget = name;
  document.getElementById('renameInput').value = name;
  document.getElementById('renameModal').classList.add('show');
  setTimeout(() => {
    const input = document.getElementById('renameInput');
    input.focus();
    const dotIndex = name.lastIndexOf('.');
    input.setSelectionRange(0, dotIndex > 0 ? dotIndex : name.length);
  }, 100);
}

function renameItem() {
  const newName = document.getElementById('renameInput').value.trim();
  if (!newName) { toast('请输入新名称', 'error'); return; }
  const path = currentPath ? currentPath + '/' + renameTarget : renameTarget;
  apiPost('rename', {path: path, newName: newName}).then(result => {
    if (result.includes('成功')) {
      toast('重命名成功');
      closeModal('renameModal');
      loadFiles();
    } else {
      toast(result, 'error');
    }
  }).catch(() => toast('重命名失败', 'error'));
}

function showDeleteModal(name, isFolder) {
  deleteTarget = {name, isFolder};
  document.getElementById('deleteName').textContent = name;
  document.getElementById('deleteModal').classList.add('show');
}

function deleteItem() {
  const path = currentPath ? currentPath + '/' + deleteTarget.name : deleteTarget.name;
  apiPost('delete', {path: path}).then(result => {
    if (result.includes('成功')) {
      toast(deleteTarget.isFolder ? '文件夹删除成功' : '文件删除成功');
      closeModal('deleteModal');
      loadFiles();
    } else {
      toast(result, 'error');
    }
  }).catch(() => toast('删除失败', 'error'));
}

// 批量删除选中项
function showDeleteSelectedModal() {
  if (selectedFiles.size === 0) return;
  const names = Array.from(selectedFiles);
  const count = names.length;
  
  // 如果只选了一个，复用原来的单个删除弹窗
  if (count === 1) {
    const name = names[0];
    const f = files.find(f => f.name === name);
    if (f) showDeleteModal(name, f.isFolder);
    return;
  }
  
  // 多个选择时，显示确认弹窗
  document.getElementById('deleteName').textContent = names.join(', ');
  document.getElementById('deleteModal').classList.add('show');
  // 修改删除按钮文字
  const deleteBtn = document.querySelector('#deleteModal .btn-danger');
  if (deleteBtn) {
    deleteBtn.textContent = `删除 (${count})`;
    deleteBtn.innerHTML = `<span class="material-icons" style="font-size:14px">delete</span>删除 (${count})`;
  }
  // 临时替换 deleteItem 函数
  window._batchDelete = true;
}

function deleteSelectedItems() {
  const names = Array.from(selectedFiles);
  let completed = 0;
  let successCount = 0;
  
  const deleteOne = (index) => {
    if (index >= names.length) {
      if (successCount > 0) {
        toast(`成功删除 ${successCount} 个项目`);
        deselectAll();
        closeModal('deleteModal');
        loadFiles();
      }
      return;
    }
    const name = names[index];
    const path = currentPath ? currentPath + '/' + name : name;
    apiPost('delete', {path: path}).then(result => {
      if (result.includes('成功')) successCount++;
      completed++;
      deleteOne(index + 1);
    }).catch(() => {
      completed++;
      deleteOne(index + 1);
    });
  };
  
  deleteOne(0);
}

// 批量移动选中项
let moveSelectedTargets = [];

function showMoveSelectedModal() {
  if (selectedFiles.size === 0) return;
  moveSelectedTargets = Array.from(selectedFiles);
  
  document.getElementById('moveSource').textContent = `已选择 ${moveSelectedTargets.length} 个项目`;
  moveTargetPath = '';
  loadFolderTree('');
  document.getElementById('moveModal').classList.add('show');
  const moveBtn = document.querySelector('#moveModal .btn-primary');
  if (moveBtn) {
    moveBtn.textContent = `移动 (${moveSelectedTargets.length})`;
  }
}

function confirmMoveSelected() {
  if (moveSelectedTargets.length === 0) return;
  
  let completed = 0;
  let successCount = 0;
  
  const moveOne = (index) => {
    if (index >= moveSelectedTargets.length) {
      if (successCount > 0) {
        toast(`成功移动 ${successCount} 个项目`);
        deselectAll();
        closeModal('moveModal');
        loadFiles();
      }
      return;
    }
    const sourcePath = currentPath ? currentPath + '/' + moveSelectedTargets[index] : moveSelectedTargets[index];
    apiPost('move', {path: sourcePath, targetDir: moveTargetPath}).then(result => {
      if (result.includes('成功')) successCount++;
      completed++;
      moveOne(index + 1);
    }).catch(() => {
      completed++;
      moveOne(index + 1);
    });
  };
  
  moveOne(0);
}

function showUploadModal() {
  document.getElementById('uploadProgress').innerHTML = '';
  document.getElementById('fileInput').value = '';
  document.getElementById('uploadModal').classList.add('show');
}

function handleFileSelect(e) {
  const files = e.target.files;
  if (files.length === 0) return;
  uploadFiles(files);
}

function uploadFiles(fileList) {
  const progress = document.getElementById('uploadProgress');
  progress.innerHTML = '';

  let completed = 0;
  const total = fileList.length;

  const uploadOne = (index) => {
    if (index >= total) return;
    const file = fileList[index];
    const itemId = 'upload-' + index;

    const itemHtml = `<div class="upload-progress-item" id="${itemId}">
      <span class="material-icons">description</span>
      <span class="file-name">${escapeHtml(file.name)}</span>
      <span class="status">上传中...</span>
      <div class="progress-bar" style="width:80px"><div class="progress-bar-fill" id="${itemId}-bar"></div></div>
    </div>`;
    progress.insertAdjacentHTML('beforeend', itemHtml);

    const formData = new FormData();
    formData.append('fileName', file);
    if (currentPath) {
      formData.append('path', currentPath);
    }

    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/main/fileUpload');
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        const percent = (e.loaded / e.total) * 100;
        document.getElementById(itemId + '-bar').style.width = percent + '%';
      }
    };
    xhr.onload = () => {
      document.getElementById(itemId).querySelector('.status').textContent = '完成';
      completed++;
      uploadOne(index + 1);
    };
    xhr.onerror = () => {
      document.getElementById(itemId).querySelector('.status').textContent = '失败';
      completed++;
      uploadOne(index + 1);
    };
    xhr.send(formData);
  };

  uploadOne(0);
}

let dragCounter = 0;

document.addEventListener('dragenter', function(e) {
  e.preventDefault();
  dragCounter++;
  document.getElementById('dropOverlay').classList.add('active');
});

document.addEventListener('dragleave', function(e) {
  e.preventDefault();
  dragCounter--;
  if (dragCounter === 0) {
    document.getElementById('dropOverlay').classList.remove('active');
  }
});

document.addEventListener('dragover', function(e) {
  e.preventDefault();
});

document.addEventListener('drop', function(e) {
  e.preventDefault();
  dragCounter = 0;
  document.getElementById('dropOverlay').classList.remove('active');

  const droppedFiles = e.dataTransfer.files;
  if (droppedFiles.length > 0) {
    showUploadModal();
    setTimeout(() => uploadFiles(droppedFiles), 100);
  }
});

const uploadArea = document.getElementById('uploadArea');
if (uploadArea) {
  uploadArea.addEventListener('dragover', e => { e.preventDefault(); e.stopPropagation(); uploadArea.classList.add('dragover'); });
  uploadArea.addEventListener('dragleave', e => { e.preventDefault(); e.stopPropagation(); uploadArea.classList.remove('dragover'); });
  uploadArea.addEventListener('drop', e => {
    e.preventDefault(); e.stopPropagation();
    uploadArea.classList.remove('dragover');
    if (e.dataTransfer.files.length > 0) uploadFiles(e.dataTransfer.files);
  });
}

function resetPreviewEncoding() {
  currentEncoding = '';
  const encodingSelect = document.getElementById('previewEncoding');
  if (encodingSelect) {
    encodingSelect.value = '';
  }
}

function closeModal(id) {
  document.getElementById(id).classList.remove('show');
  if (id === 'previewModal') {
    resetPreviewEncoding();
  }
  if (id === 'deleteModal') {
    window._batchDelete = false;
    const deleteBtn = document.querySelector('#deleteModal .btn-danger');
    if (deleteBtn) {
      deleteBtn.textContent = '删除';
      deleteBtn.innerHTML = '<span class="material-icons" style="font-size:14px">delete</span>删除';
    }
  }
  if (id === 'moveModal') {
    moveSelectedTargets = [];
    const moveBtn = document.querySelector('#moveModal .btn-primary');
    if (moveBtn) {
      moveBtn.textContent = '移动';
    }
  }
}

// 处理删除按钮点击（支持单选和批量）
function handleDelete() {
  if (window._batchDelete || selectedFiles.size > 1) {
    deleteSelectedItems();
  } else {
    deleteItem();
  }
}

// 处理移动按钮点击（支持单选和批量）
function handleMove() {
  if (moveSelectedTargets.length > 1) {
    confirmMoveSelected();
  } else {
    confirmMove();
  }
}

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'));
    hideMenu();
    resetPreviewEncoding();
  }
});

document.querySelectorAll('.modal-overlay').forEach(overlay => {
  overlay.addEventListener('click', e => {
    // 所有弹窗都只在点击关闭按钮或按 Escape 键时关闭
    // 点击遮罩层不关闭弹窗
  });
});

document.addEventListener('click', e => {
  if (!e.target.closest('.context-menu')) {
    hideMenu();
  }
  if (!e.target.closest('.file-card,.file-list-row,.context-menu,.modal-overlay,.selection-toolbar')) {
    if (!selectionMode) {
      deselectAll();
    }
    selectedFile = null;
  }
});

document.addEventListener('keydown', e => {
  if (e.target.tagName === 'INPUT') return;
  if (e.key === 'Delete' && selectedFiles.size > 0) {
    showDeleteSelectedModal();
  } else if (e.key === 'Delete' && selectedFile) {
    const f = files.find(f => f.name === selectedFile);
    if (f) showDeleteModal(f.name, f.isFolder);
  }
  if (e.key === 'F2' && selectedFiles.size === 1) {
    const name = selectedFiles.values().next().value;
    showRenameModal(name);
  }
  if (e.key === 'Enter' && selectedFiles.size === 0 && selectedFile) {
    const f = files.find(f => f.name === selectedFile);
    if (f && f.isFolder) {
      navigateTo(currentPath ? currentPath + '/' + f.name : f.name);
    } else if (f) {
      previewFile(f.name);
    }
  }
  if (e.key === 'Backspace' && !e.target.tagName === 'INPUT') {
    goUp();
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'a' && selectedFiles.size === 0) {
    e.preventDefault();
    selectAll();
  }
  if (e.key === 'Escape') {
    deselectAll();
  }
});

function goUp() {
  if (!currentPath) return;
  const parts = currentPath.split('/').filter(Boolean);
  parts.pop();
  navigateTo(parts.join('/'));
}

loadState();
// 恢复 UI 状态
document.getElementById('viewGrid').classList.toggle('active', viewMode === 'grid');
const viewPreviewBtn = document.getElementById('viewPreview');
if (viewPreviewBtn) {
  viewPreviewBtn.classList.toggle('active', viewMode === 'preview');
}
document.getElementById('viewList').classList.toggle('active', viewMode === 'list');
document.getElementById('sortSelect').value = sortBy + '-' + sortDir;
document.querySelectorAll('.filter-btn').forEach(btn => {
  btn.classList.toggle('active', btn.dataset.mode === filterMode);
});
navigateTo(currentPath);