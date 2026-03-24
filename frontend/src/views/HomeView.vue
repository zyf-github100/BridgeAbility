<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="home-hero">
    <div class="home-copy">
      <p class="eyebrow">BridgeAbility</p>
      <h2>让残障青年更顺畅地开始找工作</h2>
      <p class="hero-summary">
        在这里可以完善求职档案、查看岗位机会、获得支持服务，并持续跟进每一步进展。
      </p>
      <div class="home-actions">
        <RouterLink class="primary-link" to="/workspace">我要找工作</RouterLink>
        <RouterLink class="secondary-link" to="/help-center">查看帮助中心</RouterLink>
      </div>
      <ul class="inline-list">
        <li>档案建档</li>
        <li>岗位匹配</li>
        <li>支持协同</li>
        <li>进度反馈</li>
      </ul>
    </div>

    <div class="hero-board">
      <div class="board-header">
        <span>平台服务</span>
        <strong>在这里找到求职、招聘和支持服务</strong>
      </div>
      <div class="board-grid">
        <div>
          <p class="board-label">求职者</p>
          <strong>完善档案，查看岗位机会</strong>
          <p>填写个人档案、查看推荐岗位、跟进投递记录和当前进展。</p>
        </div>
        <div>
          <p class="board-label">企业</p>
          <strong>发布岗位并跟进招聘进展</strong>
          <p>管理认证信息、岗位发布、候选人推进和面试安排。</p>
        </div>
        <div>
          <p class="board-label">服务机构</p>
          <strong>围绕个案持续提供支持</strong>
          <p>处理建档、风险预警、干预记录和回访任务。</p>
        </div>
        <div>
          <p class="board-label">帮助与无障碍</p>
          <strong>帮助与设置随时可用</strong>
          <p>帮助中心和无障碍设置可随时查看，方便快速解决问题。</p>
        </div>
      </div>
    </div>
  </section>

  <section class="module-matrix">
    <article>
      <p class="eyebrow">岗位匹配</p>
      <h3>让推荐理由看得见</h3>
      <p>
        用匹配分、岗位摘要和推荐理由帮助用户快速判断是否适合投递。
      </p>
    </article>
    <article>
      <p class="eyebrow">支持配合</p>
      <h3>把支持需求放进求职过程里</h3>
      <p>
        求职者、企业和服务机构围绕同一条记录协作，减少重复沟通和信息断点。
      </p>
    </article>
    <article>
      <p class="eyebrow">进度追踪</p>
      <h3>关键节点都有状态反馈</h3>
      <p>
        从投递、沟通到回访，各环节都有状态和时间信息，方便持续跟进。
      </p>
    </article>
  </section>

  <section class="route-strip">
    <div class="route-list home-start-grid">
      <RouterLink to="/workspace" class="route-item">
        <strong>我要找工作</strong>
        <span>进入求职服务，完善档案、查看推荐岗位并跟进投递进展。</span>
      </RouterLink>
      <RouterLink to="/enterprise" class="route-item">
        <strong>我要招聘</strong>
        <span>进入企业服务，管理认证、岗位发布、候选人推进和面试安排。</span>
      </RouterLink>
      <RouterLink to="/help-center" class="route-item">
        <strong>帮助中心</strong>
        <span>查看账号恢复、岗位沟通、入职反馈和常见操作指引。</span>
      </RouterLink>
      <RouterLink to="/accessibility" class="route-item">
        <strong>无障碍设置</strong>
        <span>调整高对比度、大字号、键盘聚焦和减少动态效果等偏好。</span>
      </RouterLink>
    </div>
  </section>
</template>
